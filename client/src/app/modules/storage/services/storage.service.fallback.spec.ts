import { TestBed } from '@angular/core/testing';
import { STORAGE_PREFIX } from '../tokens';
import { StorageService } from './storage.service';
import { BrowserService } from '../../browser/services/browser.service';
import { CookieBuilder } from '../../browser/domain/cookie-builder';
import { Cookie } from '../../browser/domain/cookie';

describe('StorageService in fallback mode', () => {
  const mockStoragePrefix = 'provapp_';
  const mockStorageCookieName = '_' + mockStoragePrefix + 'storage';

  let service: StorageService;

  let mockSessionStorage;
  let mockBrowserService;

  beforeEach(() => {
    mockSessionStorage = jasmine.createSpyObj('Storage', ['getItem', 'setItem', 'removeItem']);
    mockBrowserService = jasmine.createSpyObj('BrowserService', ['getLocalStorage', 'getCookie', 'setCookie', 'deleteCookieByName']);

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
    service = TestBed.inject(StorageService);
  });

  beforeEach(() => {
    mockSessionStorage.setItem.and.throwError('Quota exceeded');
  });

  it('should save item to cookie if session storage is not writable', () => {
    /* given */
    mockBrowserService.getCookie.and.returnValue(null);
    /* when */
    service.saveItem('test', { hello: 'world' });
    /* then */
    expect(mockBrowserService.setCookie).toHaveBeenCalledWith(
      new CookieBuilder(mockStorageCookieName).withObjectValue({ test: JSON.stringify({ hello: 'world' }) }).build()
    );
  });

  it('should read item from cookie if session storage is not writable', () => {
    /* given */
    mockBrowserService.getCookie.and.returnValue(
      new CookieBuilder(mockStorageCookieName)
        .withObjectValue({
          test: { hello: 'world' }
        })
        .build()
    );
    /* when */
    const retrievedObject = service.getItem('test');
    /* then */
    expect(mockBrowserService.getCookie).toHaveBeenCalledWith(mockStorageCookieName);
    expect<any>(retrievedObject).toEqual({ hello: 'world' });
  });

  it('should remove item from cookie if session storage is not writable', () => {
    /* given */
    mockBrowserService.getCookie.and.returnValue(
      new CookieBuilder(mockStorageCookieName)
        .withObjectValue({
          test: { hello: 'world' },
          test2: { x: 1 }
        })
        .build()
    );
    /* when */
    service.removeItem('test');
    /* then */
    expect(mockBrowserService.getCookie).toHaveBeenCalledWith(mockStorageCookieName);
    expect(mockBrowserService.setCookie).toHaveBeenCalledWith(
      new Cookie(mockStorageCookieName, JSON.stringify({ test2: { x: 1 } }), false, null, '/')
    );
  });

  it('should remove item from cookie if session storage is not writable', () => {
    /* given */
    mockBrowserService.getCookie.and.returnValue(
      new CookieBuilder(mockStorageCookieName)
        .withObjectValue({
          test: { hello: 'world' }
        })
        .build()
    );
    service.saveItem('test', { hello: 'world' });
    /* when */
    service.removeAll();
    /* then */
    expect(mockBrowserService.deleteCookieByName).toHaveBeenCalledWith(mockStorageCookieName);
  });
});
