# Mongo DB
Mongo has a shell that allows access the DB.

## Open Mongo Shell

### STEP 1: Docker exec bash
```
docker exec -it [container id] bash
```

### STEP 2:
Mongo login
```
mongo --username admin
```

## Useful Script

### Create Database
The use [database] will create a database and switch to it.
```
use fxdb
```
The database is only created, when there is a first document insert
```
db.movie.insert({"name":"tutorials point"})
```

### User Role
```
db.createUser({ user: "fxdbuser", pwd: "fxdbuser", roles: [ { role: "readWrite", db: "fxdb"} ] })

db.updateUser({ user: "fxdbuser", pwd: "fxdbuser", roles: [ { role: "readWrite", db: "fxdb"} ] })
```