local-run:
	./gradlew clean run

local-up:
	docker run --rm --name postgres -e POSTGRES_PASSWORD=pgpassword -p 5432:5432 -d postgres

local-down:
	docker stop postgres