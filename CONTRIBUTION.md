
## Development

### Github Maven Repo Access

To be able to access additional internal libs, make sure you've setup maven for github access.

See README of agoora-agents.


### Checkout

``` bash
git clone git@github.com:spoud/agoora-agents.git
git submodule init
git submodule update
```

### Update submodule

```bash
git submodule update --force --init --recursive --remote
```


### Build

``` bash
mvn clean package -Pnative
```

#### Docker

There's a docker-compose file.

``` bash
docker-compose build
```

### Styleguide

We use the [Google Style Guide](https://google.github.io/styleguide/javaguide.html). 

To setup Intellij accordingly, we follow the recommendation of the gerrit review settings 
https://gerrit-review.googlesource.com/Documentation/dev-intellij.html#_recommended_settings
