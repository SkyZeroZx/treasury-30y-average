# Treasury 30Y Average

Spring Boot 4.1.1, WebFlux, Java 27, Gradle and Lombok.
Use JDK 21 for Gradle and install JDK 27 in `~/.jdks` for the application toolchain.

## Run

For a fresh checkout, copy `.env.example` to `.env` and set your [Alpha Vantage API key](https://www.alphavantage.co/support/#api-key). Spring loads `.env` automatically. Git ignores it; never force-add it.

```powershell
.\gradlew.bat bootRun
```

## Request

Run in another terminal (`curl` instead of `curl.exe` on macOS/Linux):

```powershell
curl.exe -i "http://localhost:8080/api/v1/treasury/30y/average?from=2026-08-12&to=2026-08-18"
```

Observed response from the live provider:

```json
{"from":"2026-08-12","to":"2026-08-18","average":5.26}
```

Inclusive dates; available yields only; two decimals, `HALF_UP`.
HTTP: `400` invalid dates, `404` no data, `502` provider failure, `504` timeout.
Manually verified with curl: valid range, single day, empty weekend, reversed range, malformed date and missing parameter.

## Build

```powershell
.\gradlew.bat build
```

This also runs the tests.

## Structure

- `modules/treasury/domain`: models and the `TreasuryServiceAdapter` port.
- `modules/treasury/usecases`: `GetTreasuryAverageUseCases`, where the average is calculated.
- `modules/treasury/infrastructure`: the REST controller and the Alpha Vantage client.
- `core`: configuration and the global error handler.
