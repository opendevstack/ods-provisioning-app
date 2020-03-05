import { CookieBuilder } from '../domain/cookie-builder';
import { BrowserService } from './browser.service';

describe('BrowserService:', () => {

  it('should provide session storage', () => {
    /* given */
    const mockDocument = {
      cookie: '',
      defaultView: {
        sessionStorage: {}
      }
    };
    const service = new BrowserService(mockDocument);
    /* when */
    const storage: any = service.getSessionStorage();
    /* then */
    expect(storage).toEqual({});
  });

  describe('handling cookies', () => {

    beforeEach(() => {
      /* when */
      jest.spyOn(Date, 'now').mockImplementation(() => new Date().getTime());
    });

    it('should set a secure cookie by name', () => {
      /* given */
      const mockDocument = {
        cookie: '',
        defaultView: {}
      };
      const service = new BrowserService(mockDocument);
      /* when */
      service.setCookie(new CookieBuilder('hello')
        .withStringValue('world')
        .asSecure()
        .build());
      /* then */
      expect(mockDocument.cookie).toEqual('hello=world;path=/;secure');
    });

    it('should set non-secure cookie by name', () => {
      /* given */
      const mockDocument = {
        cookie: '',
        defaultView: {}
      };
      const service = new BrowserService(mockDocument);
      /* when */
      service.setCookie(new CookieBuilder('hello')
        .withStringValue('world')
        .build());
      /* then */
      expect(mockDocument.cookie).toEqual('hello=world;path=/');
    });

    it('should set a secure cookie by name including an expiration date', () => {
      /* given */
      const mockDocument = {
        cookie: '',
        defaultView: {}
      };
      const service = new BrowserService(mockDocument);

      /* when */
      service.setCookie(new CookieBuilder('hello')
        .withStringValue('world')
        .asSecure()
        .withExpiringInDays(30)
        .build());
      /* then */
      expect(mockDocument.cookie).toEqual('hello=world;path=/;secure;max-age=2592000');
    });

    it('should return an empty string when no cookie is set', () => {
      /* given */
      const mockDocument = {
        cookie: '',
        defaultView: {}
      };
      const service = new BrowserService(mockDocument);
      /* when */
      const cookie = service.getCookie('hello');
      /* then */
      expect(cookie).toBeNull();
    });

    it('should return cookie value when cookie is set and cookie name exists', () => {
      /* given */
      const mockDocument = {
        cookie: 'doesnt=matter; hello=world;',
        defaultView: {}
      };
      const service = new BrowserService(mockDocument);
      /* when */
      const cookie = service.getCookie('hello');
      /* then */
      expect(cookie.value).toEqual('world');
    });

    it('should delete cookie by its name', () => {
      /* given */
      const mockDocument = {
        cookie: 'doesnt=matter; hello=world;',
        defaultView: {}
      };
      const service = new BrowserService(mockDocument);
      /* when */
      service.deleteCookieByName('hello');
      /* then */
      const deletedCookie = service.getCookie('hello');
      expect(deletedCookie.value).toEqual('');
    });

  });

});
