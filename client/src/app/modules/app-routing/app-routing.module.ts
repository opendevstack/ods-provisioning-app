import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { BrowserModule } from '../browser/browser.module';
import { environment } from '../../../environments/environment';
import { ProjectPageModule } from '../project-page/project-page.module';
import { StorageModule } from '../storage/storage.module';
import { NewProjectModule } from '../new-project/new-project.module';

const routes: Routes = [
  {
    path: 'project',
    loadChildren: () =>
      import('../project-page/project-page.module').then(
        m => m.ProjectPageModule
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
    path: 'new',
    loadChildren: () =>
      import('../new-project/new-project.module').then(m => m.NewProjectModule)
  },
  {
    path: 'about',
    loadChildren: () =>
      import('../about-page/about-page.module').then(m => m.AboutPageModule)
  },
  {
    path: '',
    redirectTo: 'project',
    pathMatch: 'full'
  },
  {
    path: '**',
    redirectTo: 'project'
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
    }),
    NewProjectModule.withOptions({
      apiProjectUrl: environment.apiProjectUrl
    })
  ],
  exports: [RouterModule],
  providers: []
})
export class AppRoutingModule {}
