import { createServiceFactory, SpectatorService } from '@ngneat/spectator/jest';
import { STORAGE_PREFIX } from '../tokens';
import { StorageService } from './storage.service';
import { BrowserService } from '../../browser/services/browser.service';

describe('StorageService in default mode', () => {
  const mockStoragePrefix = 'provapp_';

  const mockSessionStorage = {
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

  const mockBrowserService = {
    getSessionStorage: () => mockSessionStorage,
    getCookie: jest.fn(),
    setCookie: jest.fn()
  };

  let spectator: SpectatorService<StorageService>;
  const createService = createServiceFactory({
    service: StorageService,
    providers: [
      {
        provide: STORAGE_PREFIX,
        useValue: mockStoragePrefix
      },
      {
        provide: BrowserService,
        useValue: mockBrowserService
      }
    ]
  });

  beforeEach(() => {
    spectator = createService();
  });

  it('should save item to session storage', () => {
    /* when */
    spectator.service.saveItem('test', { hello: 'world' });
    /* then */
    expect(spectator.service.getItem('test')).toEqual({ hello: 'world' });
  });

  it('should remove item from session storage', () => {
    /* given */
    spectator.service.saveItem('test', { hello: 'world' });
    /* when */
    spectator.service.removeItem('test');
    /* then */
    expect(spectator.service.getItem('test')).toBeNull();
  });

  it('should remove all previously stored items from session storage', () => {
    /* given */
    spectator.service.saveItem('x', { hello: 'world' });
    spectator.service.saveItem('y', { test: 'example' });
    /* when */
    spectator.service.removeAll();
    /* then */
    expect(spectator.service.getItem('x')).toBeNull();
    expect(spectator.service.getItem('y')).toBeNull();
  });
});
