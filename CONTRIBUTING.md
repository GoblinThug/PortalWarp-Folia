# Contributing Guide

Thank you for your interest in **PortalWarp**!

**Author:** Goblin_Thug  
**Version:** 1.0.0

---

## Contents

1. [English](#english)
2. [На русском](#на-русском) *(additional)*

---

## English

### How to help

- Bug reports
- Feature ideas
- Pull requests
- Documentation improvements

### Before you start

1. Use a compatible **Paper / Folia** build (API `26.2`, Java **25**).
2. Build with `mvn clean package`.
3. Test on a local server.

### Bug reports

Include:

- PortalWarp version
- Paper / Folia and Java versions
- Steps to reproduce
- Expected vs actual behavior
- Relevant logs (without unnecessary personal data)

### Pull requests

1. Branch from the latest main line.
2. Use clear branch names, for example:
   - `fix/portal-particles`
   - `feat/connect-suggestions`
   - `docs/readme-typo`
3. Match existing Java style (4 spaces, package `ru.goblin_thug.portalwarp`).
4. Do not commit `target/`, IDE junk, or local server worlds.
5. Explain **what** and **why** in the PR description.
6. Screenshots or videos are welcome for visual changes.

### Code style

- Prefer clear class and method names.
- Avoid debug spam in the console.
- Keep player-facing messages consistent with the current codebase unless agreed otherwise.
- Do not break config keys without need; update `config.yml` and README when you do.

### Commit messages

```text
fix: stop particle tasks when portal is deleted
feat: suggest portal names on delete
docs: clarify install steps
```

### License

By contributing, you agree that your contributions will be licensed under the **MIT** License (see `LICENSE`).

---

## На русском

### Как помочь проекту

- Сообщения об ошибках (Issues)
- Предложения улучшений
- Pull Request
- Улучшение документации

### Перед началом

1. Используйте совместимую версию **Paper / Folia** (API `26.2`, Java **25**).
2. Соберите проект: `mvn clean package`.
3. Проверьте на тестовом сервере.

### Сообщение об ошибке

Укажите версию PortalWarp, Paper / Folia и Java, шаги воспроизведения, ожидаемое и фактическое поведение, фрагменты логов.

### Pull Request

1. Создайте ветку от актуальной основной линии.
2. Используйте понятные имена (`fix/...`, `feat/...`, `docs/...`).
3. Следуйте стилю кода проекта.
4. Не коммитьте `target/`, IDE-файлы и тестовые миры.
5. Опишите в PR: **что** изменено и **зачем**.

### Лицензия

Отправляя вклад, вы соглашаетесь, что он будет распространяться под лицензией **MIT**.
