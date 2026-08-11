# Online Auction Architecture

## Client-Server Overview

The application is split into a JavaFX client and a socket-based server.

- Client entry points live in `AUCTIONCODE.UI`.
- FXML views live in `src/main/resources/AUCTIONCODE/UI/view`.
- JavaFX controllers live in `AUCTIONCODE.UI.Controller`.
- Network communication is handled by `AUCTIONCODE.Network.AuctionClient`.
- Server request handling is handled by `AUCTIONCODE.Network.AuctionServer`, `ClientHandler`, and `RequestParser`.

## MVC On The Client

- View: FXML files under `src/main/resources/AUCTIONCODE/UI/view`.
- Controller: JavaFX controllers under `src/main/java/AUCTIONCODE/UI/Controller`.
- Model: domain classes under `src/main/java/AUCTIONCODE/Model`.

Controllers should keep UI state and user actions only. Business rules should stay in model, manager, authentication, or server-side request handling classes.

## Server Layers

- Network layer: accepts socket requests and routes command strings.
- Controller/service layer: `RequestParser`, `AuthService`, `AuctionManager`, and `UserManager`.
- Model layer: `AuctionRoom`, `BidTransaction`, `Item`, `User`, and related subclasses.
- DAO layer: classes under `src/main/java/AUCTIONCODE/Database` isolate SQLite access from business logic.
- Runtime SQLite data lives under `data/database.db`, outside the Java source tree.

## Quality Gates

- Maven builds the project and runs tests with JUnit 5.
- Unit tests live under `src/test/java`.
- GitHub Actions runs `./mvnw -B test` automatically on pushes and pull requests.
- GitHub Actions can also refresh the repository diagram through the `Repo Diagram` workflow.

## Build And Run

Use the Maven Wrapper so the project can build even when Maven is not installed globally.

```powershell
.\mvnw.cmd test
```

Run the JavaFX client:

```powershell
.\mvnw.cmd javafx:run
```

Build the packaged JAR:

```powershell
.\mvnw.cmd clean package
```

The main client launcher is `AUCTIONCODE.UI.MainLauncher`.

## GitHub Workflows

- `CI`: checks the project on pushes and pull requests by running the test suite.
- `Repo Diagram`: manually or automatically refreshes a repository diagram on pushes to `main` or `master`.
