# Makefile for Docker commands
rebuild:
	docker compose up --build -d

orebuild:
	docker compose build $(S)
	docker compose up -d

up:
	docker compose up -d

oup:
	docker compose up -d $(S)

stop:
	docker compose stop

ostop:
	docker compose stop $(S)

restart:
	docker compose stop 
	docker compose up -d 

down:
	@echo "Removing Docker containers..."
	docker compose down
	@echo "Docker containers removed."

odown:
	@echo "Removing container..."
	docker compose down $(S)
	@echo "Docker container removed."

exec:
	docker compose exec -it $(S) bash

logs:
	docker compose logs -f $(S)

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
	@echo "Removing images related to ${S}..."
	docker images | grep ${S} | awk '{ print $$3 }' | xargs -r docker rmi
	@echo "All ${S}-related images removed."


.PHONY: build up down logs restart clean reset rebuild exec status clean-hard orebuild stop ostop oup odown