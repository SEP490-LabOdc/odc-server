# LabOdc Platform Parent POM

Parent POM cho LabOdc Platform microservices architecture.

## Features

- Centralized dependency management
- Standardized Maven configuration
- Common build plugins
- Multi-environment profiles
- Code coverage với JaCoCo
- Docker build support

## Usage

### Build All Modules

```bash
./mvnw clean install
```

### Run Tests

```bash
./mvnw test
```

### Build with Specific Profile

```bash
./mvnw clean install -Pprod
```

### Build Docker Images

```bash
./mvnw clean package -Pdocker
```

## Profiles

- `dev` (default): Development environment
- `test`: Testing environment
- `prod`: Production environment
- `docker`: Docker build profile

## Version Management

All dependency versions are managed in the parent POM properties section.
Update versions centrally to maintain consistency across all modules.

