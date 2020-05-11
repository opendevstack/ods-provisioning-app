import { EventEmitter, Injectable, Output } from '@angular/core';
import { EditModeFlag } from '../domain/edit-mode';

@Injectable({
  providedIn: 'root'
})
export class EditModeService {
  @Output() onGetEditModeFlag = new EventEmitter<EditModeFlag>();

  constructor() {}

  emitEditModeFlag(flag: EditModeFlag): void {
    this.onGetEditModeFlag.emit(flag);
  }
}
