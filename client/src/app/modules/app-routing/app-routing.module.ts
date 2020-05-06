import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { BrowserModule } from '../browser/browser.module';
import { environment } from '../../../environments/environment';
import { ProjectPageModule } from '../project-page/project-page.module';
import { StorageModule } from '../storage/storage.module';

const routes: Routes = [
  {
    path: 'provision',
    loadChildren: () =>
      import('../provision-page/provision-page.module').then(
        m => m.ProvisionPageModule
      )
  },
  {
    path: 'project/:key',
    loadChildren: () =>
      import('../project-page/project-page.module').then(
        m => m.ProjectPageModule
      )
  },
  {
    path: 'about',
    loadChildren: () =>
      import('../about-page/about-page.module').then(m => m.AboutPageModule)
  },
  {
    path: '',
    redirectTo: 'about',
    pathMatch: 'full'
  },
  {
    path: '**',
    redirectTo: 'about'
  }
];

@NgModule({
  imports: [
    RouterModule.forRoot(routes, { enableTracing: false }),
    BrowserModule,
    StorageModule.withOptions({
      storagePrefix: 'provapp_'
    }),
    ProjectPageModule.withOptions({
      apiProjectUrl: environment.apiProjectUrl,
      apiAllProjectsUrl: environment.apiAllProjectsUrl,
      apiAllQuickstartersUrl: environment.apiAllQuickstartersUrl
    })
  ],
  exports: [RouterModule],
  providers: []
})
export class AppRoutingModule {}
