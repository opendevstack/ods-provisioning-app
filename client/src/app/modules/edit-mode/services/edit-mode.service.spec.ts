import { TestBed } from '@angular/core/testing';

import { EditModeService } from './edit-mode.service';

describe('EditModeService', () => {
  let service: EditModeService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(EditModeService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  /** editmode emits should be tested within consuming components, no need here since its default ts / angular functionality */
});
