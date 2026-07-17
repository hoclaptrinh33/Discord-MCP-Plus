#!/usr/bin/env python3
"""Clear ALL messages in one Discord channel (keep channel). Bot token from env."""
import os, sys, time, json, urllib.request, urllib.error

TOKEN = os.environ.get('DISCORD_TOKEN', '')
BASE = 'https://discord.com/api/v10'


def hdr():
    return {
        'Authorization': 'Bot ' + TOKEN,
        'User-Agent': 'DiscordBot (https://example.com, 1.0)',
        'Content-Type': 'application/json',
    }


def get_messages(cid, before=None):
    url = BASE + '/channels/' + cid + '/messages?limit=100' + (('&before=' + before) if before else '')
    req = urllib.request.Request(url, headers=hdr())
    return json.load(urllib.request.urlopen(req))


def bulk_delete(cid, ids):
    url = BASE + '/channels/' + cid + '/messages/bulk-delete'
    data = json.dumps({'messages': ids}).encode()
    req = urllib.request.Request(url, data=data, headers=hdr(), method='POST')
    urllib.request.urlopen(req)
    return len(ids)


def delete_individual(cid, ids):
    cnt = 0
    for mid in ids:
        url = BASE + '/channels/' + cid + '/messages/' + mid
        req = urllib.request.Request(url, headers=hdr(), method='DELETE')
        try:
            urllib.request.urlopen(req)
            cnt += 1
        except urllib.error.HTTPError as e:
            if e.code == 429:
                time.sleep(int(e.headers.get('Retry-After', '1')))
                try:
                    urllib.request.urlopen(req)
                    cnt += 1
                except Exception:
                    pass
            else:
                print('  skip ' + mid + ': HTTP ' + str(e.code))
        time.sleep(0.4)
    return cnt


def main():
    cid = sys.argv[1]
    total = 0
    before = None
    while True:
        try:
            msgs = get_messages(cid, before)
        except urllib.error.HTTPError as e:
            print('GET err', e.code, e.read().decode()[:200])
            break
        if not msgs:
            break
        ids = [m['id'] for m in msgs]
        try:
            n = bulk_delete(cid, ids)
        except urllib.error.HTTPError as e:
            if e.code == 400:
                n = delete_individual(cid, ids)
            elif e.code == 429:
                time.sleep(int(e.headers.get('Retry-After', '1')))
                continue
            else:
                print('bulk err', e.code, e.read().decode()[:200])
                break
        total += n
        print('  deleted ' + str(n) + ' (total ' + str(total) + ')')
        before = msgs[-1]['id']
        time.sleep(1.0)
    print('DONE total =', total)


if __name__ == '__main__':
    main()
