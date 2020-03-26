import {Component, OnDestroy, OnInit, Renderer2} from '@angular/core';
import {Subject} from "rxjs";

@Component({
  selector: 'app-loading-indicator',
  templateUrl: './loading-indicator.component.html'
})
export class LoadingIndicatorComponent implements OnInit, OnDestroy {

  private destroy$: Subject<boolean> = new Subject<boolean>();

  constructor(private renderer: Renderer2) { }

  ngOnInit(): void {
    this.renderer.addClass(document.body, 'is-loading');
  }

  ngOnDestroy(): void {
    this.renderer.removeClass(document.body, 'is-loading');
    this.destroy$.next(true);
    this.destroy$.unsubscribe();
  }

}
