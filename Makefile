.PHONY: dev infra-up infra-down test verify compose-check

dev:
	docker compose up --build

infra-up:
	docker compose up -d postgres

infra-down:
	docker compose down

test:
	cd backend && ./mvnw test
	cd frontend && npm run test:run

verify:
	cd backend && ./mvnw verify
	cd frontend && npm run check

compose-check:
	docker compose config --quiet
