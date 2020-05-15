import { createServiceFactory, SpectatorService } from '@ngneat/spectator/jest';
import { EditMode } from './edit-mode.service';

describe('EditModeService', () => {
  let spectator: SpectatorService<EditMode>;

  const createService = createServiceFactory({
    service: EditMode
  });

  let onGetEditModeFlagSpy: any;

  beforeEach(() => {
    spectator = createService();
    onGetEditModeFlagSpy = spyOn(spectator.service.onGetEditModeFlag, 'emit');
  });

  it('should be created', () => {
    expect(spectator.service).toBeTruthy();
  });

  it('should emit editmode event with "true" flag to host component', () => {
    /* given */
    /* when */
    spectator.service.enabled = true;
    /* then */
    expect(onGetEditModeFlagSpy).toBeCalledWith(true);
  });

  it('should emit editmode event with "false" flag to host component', () => {
    /* given */
    /* when */
    spectator.service.enabled = false;
    /* then */
    expect(onGetEditModeFlagSpy).toBeCalledWith(false);
  });
});
