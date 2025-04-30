# Makefile for Docker commands
rebuild:
	docker compose up --build

orebuild:
	docker compose build $(SERVICE_NAME)
	docker compose up -d

up:
	docker compose up -d

oup:
	docker compose up -d $(SERVICE_NAME)

stop:
	docker compose stop

ostop:
	docker compose stop $(SERVICE_NAME)

restart:
	docker compose stop 
	docker compose up -d 

down:
    @echo "Removing Docker containers..."
	docker compose down
	@echo "Docker containers removed."

odown:
	@echo "Removing container..."
	docker compose down $(SERVICE_NAME)
	@echo "Docker container removed."

exec:
	docker compose exec -it $(SERVICE_NAME) bash

logs:
	docker compose logs -f $(SERVICE_NAME)

status:
	docker compose ps

reset:
	docker compose down -v 
	docker compose up -d

clean:
	@echo "Cleaning up containers, volumes, and dangling images..."
	docker compose down --volumes --remove-orphans
	docker volume prune -f
	docker images -f "dangling=true" -q | xargs -r docker rmi
	@echo "Containers, volumes, and dangling images cleaned."

clean-hard: clean
	@echo "Removing images related to ${SERVICE_NAME}..."
	docker images | grep ${SERVICE_NAME} | awk '{ print $$3 }' | xargs -r docker rmi
	@echo "All ${SERVICE_NAME}-related images removed."


.PHONY: build up down logs restart clean reset rebuild exec status clean-hard orebuild stop ostop oup odown