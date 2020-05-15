import {
  EventEmitter,
  Injectable,
  Output,
  Renderer2,
  RendererFactory2
} from '@angular/core';
import { EditModeFlag } from '../domain/edit-mode';

@Injectable({
  providedIn: 'root'
})
export class EditMode {
  @Output() onGetEditModeFlag = new EventEmitter<EditModeFlag>();

  private _enabled = false;
  private _context = '';

  renderer: Renderer2;

  constructor(private rendererFactory2: RendererFactory2) {
    this.renderer = this.rendererFactory2.createRenderer(null, null);
    this.renderer.listen('window', 'keydown', (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        this.enabled = false;
      }
    });
  }

  set enabled(enabled: boolean) {
    this._enabled = enabled;
    this.emitEditModeFlag({
      enabled,
      context: this._context
    });
  }

  get enabled(): boolean {
    return this._enabled;
  }

  set context(context: string) {
    this._context = context;
  }

  get context(): string {
    return this._context;
  }

  private emitEditModeFlag(flag: EditModeFlag) {
    this.onGetEditModeFlag.emit(flag);
  }
}
