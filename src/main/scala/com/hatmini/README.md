## Hat Mini Akka-HTTP Server

# Introduction
This exposes a mini API mimicing the HAT server

# Improvements
- Refactor bootstrap code, split out routes
- Change permissions model to be based on dynamically assigned scopes
- Decouple Mongo logic from PDAService
- Unit, integration tests
- Create docker-compose setup
- AWS deployment
- PUT endpoints for modifying resources
- postman collection for API testing
- React frontend to interact with API
- Improved secret management for config