import { CookieBuilder } from './cookie-builder';

describe('CookieBuilder', () => {
  beforeEach(() => {
    spyOn(Date, 'now');
  });

  it('should build cookie from string', () => {
    /* when */
    const cookie = new CookieBuilder('foocookie').withStringValue('hello').build();

    /* then */
    expect(cookie.name).toEqual('foocookie');
    expect(cookie.value).toEqual('hello');
    expect(cookie.secure).toEqual(false);
    expect(cookie.maxAge).toEqual(null);
    expect(cookie.path).toEqual('/');
  });

  describe('building cookie from object', () => {
    it('should build insecure cookie without expiration date', () => {
      /* when */
      const cookie = new CookieBuilder('foocookie').withObjectValue({ hello: 'world' }).build();

      /* then */
      expect(cookie.name).toEqual('foocookie');
      expect(cookie.value).toEqual('{"hello":"world"}');
      expect(cookie.secure).toEqual(false);
      expect(cookie.maxAge).toEqual(null);
      expect(cookie.path).toEqual('/');
    });

    it('should build secure cookie with expiration date', () => {
      /* when */
      const cookie = new CookieBuilder('foocookie').withObjectValue({ hello: 'world' }).withExpiringInDays(100).asSecure().build();

      /* then */
      expect(cookie.name).toEqual('foocookie');
      expect(cookie.value).toEqual('{"hello":"world"}');
      expect(cookie.secure).toEqual(true);
      expect(cookie.maxAge).toEqual(86400 * 100);
      expect(cookie.path).toEqual('/');
    });
  });
});
