import { createServiceFactory, SpectatorService } from '@ngneat/spectator/jest';
import { EditModeService } from './edit-mode.service';

describe('EditModeService', () => {
  let spectator: SpectatorService<EditModeService>;

  const createService = createServiceFactory({
    service: EditModeService
  });

  let getEditModeFlagSpy: any;

  beforeEach(() => {
    spectator = createService();
    getEditModeFlagSpy = spyOn(spectator.service.getEditModeFlag, 'emit');
  });

  it('should be created', () => {
    expect(spectator.service).toBeTruthy();
  });

  it('should emit editmode event with "true" flag to host component', () => {
    /* given */
    /* when */
    spectator.service.enabled = true;
    /* then */
    expect(getEditModeFlagSpy).toBeCalledWith(true);
  });

  it('should emit editmode event with "false" flag to host component', () => {
    /* given */
    /* when */
    spectator.service.enabled = false;
    /* then */
    expect(getEditModeFlagSpy).toBeCalledWith(false);
  });
});
