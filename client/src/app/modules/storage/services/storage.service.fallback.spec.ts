import { inject } from '@angular/core/testing';
import { TestBed } from '@angular/core/testing';
import { STORAGE_PREFIX } from '../tokens';
import { StorageService } from './storage.service';
import {BrowserService} from "../../browser/services/browser.service";
import {CookieBuilder} from "../../browser/domain/cookie-builder";
import {Cookie} from "../../browser/domain/cookie";
import {throwError} from "rxjs";

describe('StorageService in fallback mode', () => {

  const mockStoragePrefix = 'provapp_';
  const mockStorageCookieName = '_' + mockStoragePrefix + 'storage';

  let mockSessionStorage: any;
  let mockBrowserService: any;

  beforeEach(() => {
    mockSessionStorage = {
      getItem: jest.fn(),
      setItem: jest.fn(),
      removeItem: jest.fn()
    };
    mockBrowserService = {
      getCookie: jest.fn(),
      setCookie: jest.fn(),
      getSessionStorage: jest.fn(),
      deleteCookieByName: jest.fn()
    };

    TestBed.configureTestingModule({
      providers: [
        {
          provide: STORAGE_PREFIX,
          useValue: mockStoragePrefix
        },
        {
          provide: BrowserService,
          useValue: mockBrowserService
        },
        StorageService
      ]
    });
  });

  beforeEach(() => {
    mockSessionStorage.setItem.mockImplementation(() => throwError('Quota exceeded'));
  });

  it('should save item to cookie if session storage is not writable',
    inject([StorageService], (storageService: StorageService) => {
      /* given */
      mockBrowserService.getCookie.mockReturnValue(null);
      /* when */
      storageService.saveItem('test', { hello: 'world' });
      /* then */
      expect(mockBrowserService.setCookie).toHaveBeenCalledWith(new CookieBuilder(mockStorageCookieName)
        .withObjectValue({ test: JSON.stringify({ hello: 'world' }) })
        .build());
    }));

  it('should read item from cookie if session storage is not writable',
    inject([StorageService], (storageService: StorageService) => {
      /* given */
      mockBrowserService.getCookie.mockReturnValue(
        new CookieBuilder(mockStorageCookieName)
          .withObjectValue({
            test: { hello: 'world' }
          })
          .build());
      /* when */
      const retrievedObject = storageService.getItem('test');
      /* then */
      expect(mockBrowserService.getCookie).toHaveBeenCalledWith(mockStorageCookieName);
      expect<any>(retrievedObject).toEqual({ hello: 'world' });
    }));

  it('should remove item from cookie if session storage is not writable',
    inject([StorageService], (storageService: StorageService) => {
      /* given */
      mockBrowserService.getCookie.mockReturnValue(
        new CookieBuilder(mockStorageCookieName)
          .withObjectValue({
            test: { hello: 'world' },
            test2: { x: 1 }
          })
          .build());
      /* when */
      storageService.removeItem('test');
      /* then */
      expect(mockBrowserService.getCookie).toHaveBeenCalledWith(mockStorageCookieName);
      expect(mockBrowserService.setCookie).toHaveBeenCalledWith(new Cookie(mockStorageCookieName,
        JSON.stringify({ test2: { x: 1 } }),
        false,
        null,
        '/'));
    }));

  it('should remove item from cookie if session storage is not writable',
    inject([StorageService], (storageService: StorageService) => {
      /* given */
      mockBrowserService.getCookie.mockReturnValue(
        new CookieBuilder(mockStorageCookieName)
          .withObjectValue({
            test: { hello: 'world' }
          })
          .build());
      storageService.saveItem('test', { hello: 'world' });
      /* when */
      storageService.removeAll();
      /* then */
      expect(mockBrowserService.deleteCookieByName).toHaveBeenCalledWith(mockStorageCookieName);
    }));

});
