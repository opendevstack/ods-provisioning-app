import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { BrowserModule } from '../browser/browser.module';
import { environment } from '../../../environments/environment';
import { ProjectPageModule } from '../project-page/project-page.module';
import { StorageModule } from '../storage/storage.module';
import { NewProjectModule } from '../new-project/new-project.module';
import { ProjectModule } from '../project/project.module';
import { AuthenticationModule } from '../authentication/authentication.module';

const routes: Routes = [
  {
    path: 'project',
    loadChildren: () => import('../project-page/project-page.module').then(m => m.ProjectPageModule)
  },
  {
    path: 'project/:key',
    loadChildren: () => import('../project-page/project-page.module').then(m => m.ProjectPageModule)
  },
  {
    path: 'new',
    loadChildren: () => import('../new-project/new-project.module').then(m => m.NewProjectModule)
  },
  {
    path: 'about',
    loadChildren: () => import('../about-page/about-page.module').then(m => m.AboutPageModule)
  },
  {
    path: 'login',
    loadChildren: () => import('../login/login.module').then(m => m.LoginModule)
  },
  {
    path: 'do-logout',
    loadChildren: () => import('../logout/logout.module').then(m => m.LogoutModule)
  },
  {
    path: 'error',
    loadChildren: () => import('../general-error-page/general-error-page.module').then(m => m.GeneralErrorPageModule)
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
    RouterModule.forRoot(routes, {
      enableTracing: false,
      relativeLinkResolution: 'legacy'
    }),
    BrowserModule,
    StorageModule.withOptions({
      storagePrefix: 'provapp_'
    }),
    ProjectPageModule.withOptions({
      apiAllQuickstartersUrl: environment.apiAllQuickstartersUrl
    }),
    ProjectModule.withOptions({
      apiProjectDetailUrl: environment.apiProjectDetailUrl,
      apiProjectUrl: environment.apiProjectUrl,
      apiProjectTemplatesUrl: environment.apiProjectTemplatesUrl,
      apiGenerateProjectKeyUrl: environment.apiGenerateProjectKeyUrl
    }),
    NewProjectModule,
    AuthenticationModule.withOptions({
      apiAuthUrl: environment.apiAuthUrl,
      apiLogoutUrl: environment.apiLogoutUrl
    })
  ],
  exports: [RouterModule]
})
export class AppRoutingModule {}
