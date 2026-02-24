# Build Variants

Five build variants exist, each with different API endpoints and configurations:

- `dev`: Development variant with relaxed tree data accuracy requirements. Use this for local development.
- `debug`: Standard debug build with test environment settings and blur detection enabled.
- `beta`: Testing variant pointing to test environment. Used for internal releases to testers.
- `prerelease`: Pre-production build using production environment.
- `release`: Production build with minification enabled and production API endpoints.
