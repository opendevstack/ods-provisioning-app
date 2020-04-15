import { ProjectService } from './project.service';
import { API_ALL_PROJECTS_URL, API_PROJECT_URL } from '../tokens';
import { createHttpFactory, SpectatorHttp } from '@ngneat/spectator/jest';
import { HTTPMethod } from '@ngneat/spectator';
import { Project } from '../domain/project';

describe('ProjectService', () => {
  const projectService: () => SpectatorHttp<ProjectService> = createHttpFactory<
    ProjectService
  >({
    service: ProjectService,
    providers: [
      { provide: API_PROJECT_URL, useValue: '/api/project/FOO' },
      { provide: API_ALL_PROJECTS_URL, useValue: '/api/project' }
    ]
  });

  it('should be created', () => {
    expect(projectService()).toBeTruthy();
  });

  it('should get all projects', done => {
    const { service, expectOne } = projectService();
    const mockResponse = {
      FOO1: {},
      FOO2: {},
      FOO3: {}
    };
    const mockResult = {
      FOO1: {},
      FOO2: {},
      FOO3: {}
    };
    service.getAllProjects().subscribe((projects: Project[]) => {
      expect(projects).toEqual(mockResult);
      done();
    });
    expectOne('/api/project', HTTPMethod.GET).flush(mockResponse);
  });

  it('should log an error when getting all projects fails', done => {
    const { service, expectOne } = projectService();
    const mockResponse = null;
    service.getAllProjects().subscribe(
      () => fail(),
      (error: any) => {
        expect(error).toBe('Backend returned 404');
        done();
      }
    );
    expectOne('/api/project', HTTPMethod.GET).flush(null, {
      status: 404,
      statusText: ''
    });
  });

  it('should get a single project by its key', done => {
    const { service, expectOne } = projectService();
    const mockResponse = { projectName: 'FOO' };
    const mockResult = { projectName: 'FOO' };
    service.getProjectByKey('FOO').subscribe((project: Project) => {
      expect(project).toEqual(mockResult);
      done();
    });
    expectOne('/api/project/FOO', HTTPMethod.GET).flush(mockResponse);
  });

  it('should log an error when getting a single project by its key fails', done => {
    const { service, expectOne } = projectService();
    const mockResponse = null;
    service.getProjectByKey('FOO').subscribe(
      () => fail(),
      (error: any) => {
        expect(error).toBe('Backend returned 404');
        done();
      }
    );
    expectOne('/api/project/FOO', HTTPMethod.GET).flush(null, {
      status: 404,
      statusText: ''
    });
  });

  /* TODO add tests for client errors */
});
