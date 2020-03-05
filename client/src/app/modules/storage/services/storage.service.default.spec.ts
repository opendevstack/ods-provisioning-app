import { inject } from '@angular/core/testing';
import { TestBed } from '@angular/core/testing';
import { STORAGE_PREFIX } from '../tokens';
import { StorageService } from './storage.service';
import {BrowserService} from "../../browser/services/browser.service";

describe('StorageService in default mode', () => {

  const mockStoragePrefix = 'provapp_';

  let mockSessionStorage: any;
  let mockBrowserService: any;

  mockSessionStorage = {
    getItem(key: string): any {
      return this[key];
    },
    setItem(key: string, data: string): void {
      this[key] = data;
    },
    removeItem(key: string): void {
      delete this[key];
    }
  };

  beforeEach(() => {
    mockBrowserService = {
      getSessionStorage: jest.fn()
    };

    mockBrowserService.getSessionStorage.mockReturnValue(mockSessionStorage);

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

  it('should save item to session storage',
    inject([StorageService], (storageService: StorageService) => {
      /* when */
      storageService.saveItem('test', { hello: 'world' });
      /* then */
      expect(storageService.getItem('test')).toEqual({ hello: 'world' });
    }));

  it('should remove item from session storage',
    inject([StorageService], (storageService: StorageService) => {
      /* given */
      storageService.saveItem('test', { hello: 'world' });
      /* when */
      storageService.removeItem('test');
      /* then */
      expect(storageService.getItem('test')).toBeNull();
    }));

  it('should remove all previously stored items from session storage',
    inject([StorageService], (storageService: StorageService) => {
      /* given */
      storageService.saveItem('x', { hello: 'world' });
      storageService.saveItem('y', { test: 'example'});
      /* when */
      storageService.removeAll();
      /* then */
      expect(storageService.getItem('x')).toBeNull();
      expect(storageService.getItem('y')).toBeNull();
    }));

  describe('without previously saved legacy keys', () => {

    beforeEach(() => {
      mockSessionStorage.setItem(mockStoragePrefix + 'x', JSON.stringify('foo'));
      mockSessionStorage.setItem(mockStoragePrefix + 'y', JSON.stringify('bar'));
    });

    it('should remove all previously stored items from session storage after reload',
      inject([StorageService], (storageService: StorageService) => {
        /* when */
        storageService.removeAll();
        /* then */
        expect(storageService.getItem('x')).toBeNull();
        expect(storageService.getItem('y')).toBeNull();
      }));
  });
});
