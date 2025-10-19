# 뉴스 피드 시스템

> Pull Model 기반 Fanout 전략과 Redis 캐싱을 적용하여 구현한 뉴스 피드 시스템

![Java](https://img.shields.io/badge/Java-21-orange.svg)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.0-brightgreen.svg)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-336791.svg)
![Redis](https://img.shields.io/badge/Redis-7-DC382D.svg)

## 실행 방법

### Docker Compose 실행

```bash
git clone https://github.com/mango606/newsfeed
cd newsfeed
docker-compose up --build
```

브라우저에서 `http://localhost:8080` 접속

### 로컬 실행

```bash
# PostgreSQL, Redis 실행 필요
./gradlew bootRun
```

## 기술 스택

- Java 21
- Spring Boot 3.2.0
- PostgreSQL 16
- Redis 7
- Thymeleaf

## 주요 기능

- 뉴스 피드 조회 (페이지네이션)
- 포스트 작성
- 팔로우/언팔로우
- 좋아요
- Redis 기반 피드 캐싱

## 구현 기능

### Fanout 전략

| 항목 | Push Model | Pull Model (채택) |
|------|-----------|-------------------|
| 쓰기 성능 | O(팔로워 수) | O(1) |
| 읽기 성능 | O(1) | O(팔로잉 수) |
| 저장 공간 | 중복 저장 | 단일 저장 |
| 일관성 | 언팔로우 시 복잡 | 즉시 반영 |

Pull Model을 선택한 이유
- 일반 사용자 중심 서비스에 적합
- 팔로우/언팔로우 시 즉시 반영
- 저장 공간 효율적

### Redis 캐싱 전략

**캐시 키 설계**
```
newsfeed:{userId}:{pageNumber}
예: newsfeed:1:0 (1번 유저의 0페이지)
```

**캐시 무효화 시점**
- 새 포스트 작성 시
- 팔로우/언팔로우 시
- TTL: 5분

## 구현 기능 세부사항

| 기능 | 구현 방식 |
|------|-----------|
| Fanout 전략 | Pull Model (Fanout on Read) |
| 캐싱 | Redis 페이지별 캐싱 (5분 TTL) |
| 페이지네이션 | Offset 기반 (10개/페이지) |
| 타임라인 정렬 | 생성시간 내림차순 |
| 팔로우 관계 | JPA ManyToMany 양방향 |