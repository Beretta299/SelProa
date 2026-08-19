.DEFAULT_GOAL := help
.PHONY: help up down check fmt lint types test contract evals deploy clean

help: ## Show this help
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-12s\033[0m %s\n", $$1, $$2}'

up: ## Start everything locally
	docker compose -f infra/docker-compose.yml up -d --build

down: ## Stop everything
	docker compose -f infra/docker-compose.yml down

check: fmt lint types test ## Everything CI runs. Keep this identical locally and in CI.

fmt: ## Format
	@echo "not yet — chapter 4"

lint: ## Lint
	@echo "not yet — chapter 4"

types: ## Type check (strict mypy, tsc)
	@echo "not yet — chapter 4"

test: ## Unit and integration tests (testcontainers)
	@echo "not yet — chapter 8"

contract: ## Contract tests against the real market-api container
	@echo "not yet — chapter 13"

evals: ## The evaluation suite — retrieval, answers, tool calls, extraction
	@echo "not yet — chapter 28"

deploy: ## Build, push, restart, health check
	@echo "not yet — chapter 14"

clean: ## Remove build artefacts
	find . -type d -name __pycache__ -prune -exec rm -rf {} +
	find . -type d -name .pytest_cache -prune -exec rm -rf {} +
