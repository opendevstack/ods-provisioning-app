import { STORAGE_PREFIX } from '../tokens';
import { StorageService } from './storage.service';
import { BrowserService } from '../../browser/services/browser.service';
import { TestBed } from '@angular/core/testing';

describe('StorageService in default mode', () => {
  const mockStoragePrefix = 'provapp_';

  let service: StorageService;
  let mockBrowserService;

  beforeEach(() => {
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

  it('should save item to session storage', () => {
    /* when */
    service.saveItem('test', { hello: 'world' });
    /* then */
    expect(service.getItem('test')).toEqual({ hello: 'world' });
  });

  it('should remove item from session storage', () => {
    /* given */
    service.saveItem('test', { hello: 'world' });
    /* when */
    service.removeItem('test');
    /* then */
    expect(service.getItem('test')).toBeNull();
  });

  it('should remove all previously stored items from session storage', () => {
    /* given */
    service.saveItem('x', { hello: 'world' });
    service.saveItem('y', { test: 'example' });
    /* when */
    service.removeAll();
    /* then */
    expect(service.getItem('x')).toBeNull();
    expect(service.getItem('y')).toBeNull();
  });
});
