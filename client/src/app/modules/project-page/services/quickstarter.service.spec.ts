import { TestBed } from '@angular/core/testing';

import { QuickstarterService } from './quickstarter.service';

describe('QuickstarterService', () => {
  let service: QuickstarterService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(QuickstarterService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
