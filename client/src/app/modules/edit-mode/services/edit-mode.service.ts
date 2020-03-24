import { EventEmitter, Injectable, Output } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class EditModeService {
  @Output() onGetEditModeFlag: EventEmitter<boolean> = new EventEmitter<
    boolean
  >();

  constructor() {}

  emitEditModeFlag(flag: boolean): void {
    this.onGetEditModeFlag.emit(flag);
  }
}
