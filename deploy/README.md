# 배포 · 환경변수 구성 (deploy/)

환경별 docker-compose 파일과 env 파일을 한곳에 모아둔다. 서버(호스트)마다 **루트 `.env` 한 파일**로
"어느 환경을 띄울지"만 지정하면, 어디서든 **플래그·파일 이름변경 없이** 동일하게 실행한다.

```
docker compose up -d          # 어떤 서버든 이 한 줄
```

## 구조

```
deploy/
├─ compose.local.yml      # 로컬: redis + mailhog (앱은 Gradle/IDE 로 실행)
├─ compose.staging.yml    # 스테이징: mysql + redis + app
├─ compose.prod.yml       # 운영: mysql + redis + app (리소스 제한 포함)
└─ env/
   ├─ .env.example            # 앱 환경변수 템플릿 (git 추적)
   ├─ .env.selector.example   # 루트 .env 템플릿 (git 추적)
   ├─ .env.local              # 로컬 앱 환경변수 (gitignore)
   ├─ .env.staging            # 스테이징 시크릿 (gitignore)
   └─ .env.prod               # 운영 시크릿 (gitignore)
.env                       # 루트 셀렉터 (호스트별, gitignore)
Dockerfile                # 루트 유지
```

## 동작 원리 (왜 이렇게 하나)

docker compose 는 플래그가 없으면 **루트 `.env` 파일만** 자동으로 읽는다. 그래서 루트 `.env` 에
`COMPOSE_FILE` 로 띄울 compose 파일을 지정하면, 서버마다 `.env.prod`→`.env` 식으로 **이름을 바꿀 필요가 없다.**

- `COMPOSE_FILE` : 사용할 compose 파일 경로. 루트 `.env` 에서 인식된다. ✅ 검증됨
- `COMPOSE_ENV_FILES` / `--env-file` : **쓰지 않는다.** 이걸로 다른 env 파일을 지정하면 그 순간
  루트 `.env` 가 대체되어 `COMPOSE_FILE` 지정이 무시된다(=오히려 깨짐). ❌ 검증됨
- 그래서 **치환용 인프라 변수는 루트 `.env` 안에** 두고, 앱 컨테이너 설정은 각 compose 의
  `env_file: env/.env.<환경>` 로 주입한다. (compose 치환 소스와 컨테이너 주입은 별개 경로다.)

## 호스트별 최초 설정

### 로컬
```bash
cp deploy/env/.env.selector.example .env      # COMPOSE_FILE=deploy/compose.local.yml
cp deploy/env/.env.example deploy/env/.env.local   # 이미 있으면 생략, 실제 값 채우기
docker compose up -d                          # redis + mailhog 기동
```
> 앱은 `./gradlew bootRun`(프로필 `local`, H2)으로 실행한다. **로컬 앱 환경변수는 이제
> `deploy/env/.env.local` 에 있다.** IntelliJ EnvFile 플러그인 등으로 예전 루트 `.env` 를 읽어
> 실행했다면, 그 경로를 `deploy/env/.env.local` 로 바꿔줄 것.

### 스테이징 / 운영 (EC2)
```bash
# 1) 최신 코드 반영 (deploy/ 구조 포함)
git pull

# 2) 앱 시크릿 파일을 새 위치로
#    - 기존 서버에 루트 .env.prod 가 있으면 옮기기:  mv .env.prod deploy/env/.env.prod
#    - 없으면 템플릿 복사 후 값 채우기:              cp deploy/env/.env.example deploy/env/.env.prod

# 3) 루트 셀렉터 생성
cp deploy/env/.env.selector.example .env
#    → COMPOSE_FILE=deploy/compose.prod.yml (staging 이면 compose.staging.yml)
#    → DB_*, REDIS_PASSWORD 5개 주석 해제 후 값 입력 (deploy/env/.env.prod 값과 일치)

# 4) 이미지 받아서 기동 (빌드는 CI/로컬, 여기선 pull 후 실행)
docker compose pull && docker compose up -d
```

### 루트 `.env` 예시 (운영)
```dotenv
COMPOSE_FILE=deploy/compose.prod.yml
# compose 치환용 (deploy/env/.env.prod 의 같은 값과 일치시킬 것)
DB_NAME=harucut
DB_USERNAME=harucut
DB_PASSWORD=********
DB_ROOT_PASSWORD=********
REDIS_PASSWORD=********
```

## 프로젝트명

각 compose 파일에 `name: harucut_be` 를 고정했다. compose 파일이 `deploy/` 하위에 있어
지정하지 않으면 프로젝트명이 폴더명 `deploy` 가 되고, 볼륨/컨테이너 접두사도 그걸 따른다.
고정해 두면 어느 위치에서 실행하든 `harucut_be_mysql_data` 처럼 일관된 이름이 된다.

## 환경변수 소스 요약

| 변수 | 위치 | 용도 |
|------|------|------|
| `COMPOSE_FILE` | 루트 `.env` | 띄울 compose 선택 |
| `IMAGE_TAG` (`APP_IMAGE`) | 루트 `.env` | 배포할 app 이미지 버전(레지스트리 태그). 생략 시 `latest` |
| `DB_NAME/USERNAME/PASSWORD`, `DB_ROOT_PASSWORD`, `REDIS_PASSWORD` | 루트 `.env` | mysql/redis 컨테이너 `${...}` 치환 |
| `DB_HOST`, `REDIS_HOST`, `SPRING_PROFILES_ACTIVE` | compose `environment:` | 컨테이너 네트워크명 고정 |
| 앱 전체 시크릿 (JWT/OAuth/AWS/MAIL/PAYMENT/S3/DB_*/REDIS_* …) | `deploy/env/.env.<환경>` | 앱 컨테이너 `env_file` 주입 |
