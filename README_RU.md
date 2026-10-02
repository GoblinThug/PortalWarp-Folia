# PortalWarp

<p align="center">
  <strong>Плагин порталов-варпов для Paper / Folia</strong><br>
  Создавайте связанные рамки-порталы в мире и межсерверные переходы одной командой.
</p>

<p align="center">
  <a href="README.md">English</a> ·
  <a href="#установка">Установка</a> ·
  <a href="#команды">Команды</a> ·
  <a href="#конфигурация">Конфиг</a> ·
  <a href="CHANGELOG.md">Changelog</a> ·
  <a href="LICENSE">MIT License</a>
</p>

---

| | |
|---|---|
| **Версия** | `1.0.0` |
| **Автор** | **Goblin_Thug** |
| **Платформа** | Paper / Folia (API `26.2`) |
| **Java** | 25+ |
| **Сборка** | Maven |
| **Лицензия** | MIT |

> Основная документация — на [английском (`README.md`)](README.md). Этот файл — русская версия.

---

## Описание

**PortalWarp** — плагин для серверов на базе **Paper** и **Folia**, который позволяет строить порталы из рамок блоков и связывать их между собой.

Поддерживаются два режима:

1. **Локальный варп** — две рамки в одном или разных мирах. Вход в одну телепортирует ко второй и наоборот.
2. **Серверный (межсерверный) портал** — одна рамка. При входе игрок перенаправляется на указанный адрес сервера (`ip:port` или хост).

Рамки выбираются **возвратным компасом** (`RECOVERY_COMPASS`): плагин сам анализирует контур, заполняет интерьер и запускает эффекты частиц.

---

## Возможности

- Создание двусторонних порталов по названию
- Межсерверные порталы для администрации
- Автораспознавание рамки в плоскостях **XY / XZ / YZ**
- Заполнение интерьера (по умолчанию — `CYAN_STAINED_GLASS_PANE`)
- Настраиваемые частицы портала (цвет, размер, частота)
- Кулдаун после телепортации
- Ограничение размера интерьера
- Сохранение порталов в `config.yml`
- Команды на **Brigadier** (подсказки названий при удалении)
- Полная поддержка **Folia** (региональные планировщики)
- Права доступа для игроков и админов

---

## Требования

| Компонент | Требование |
|-----------|------------|
| Сервер | Paper или Folia с API **26.2** |
| Java | **25** или новее |
| Сборка (для разработчиков) | Maven 3.9+ |
| Предмет для создания | Возвратный компас |

> Плагин использует Folia API (`dev.folia:folia-api`) со scope `provided` — на сервере отдельный jar зависимостей не нужен.

---

## Установка

1. Соберите jar (см. [Сборка](#сборка)) или скачайте релиз `PortalWarp-1.0.0.jar`.
2. Положите файл в папку `plugins/` сервера.
3. Перезапустите сервер (или загрузите плагин совместимым менеджером, если используете).
4. При необходимости отредактируйте `plugins/PortalWarp/config.yml`.
5. Выполните `/portal reload` (нужно право `portalwarp.admin`).

Структура после первого запуска:

```text
plugins/
└── PortalWarp/
    └── config.yml
```

---

## Быстрый старт

### Локальный портал (варп)

1. Постройте **две** замкнутые рамки (например, из обсидиана) с пустым интерьером.
2. Возьмите в основную руку **возвратный компас**.
3. Выполните:
   ```text
   /portal warp create мой_портал
   ```
4. ПКМ компасом по **первой** рамке (выход / «откуда»).
5. ПКМ компасом по **второй** рамке (вход / «куда»).
6. Готово: зайдите в заполненный интерьер — вас телепортирует на вторую сторону.

### Межсерверный портал

1. Постройте **одну** рамку.
2. Возьмите возвратный компас.
3. Выполните (нужно `portalwarp.admin`):
   ```text
   /portal connect create хаб "play.example.com:25565"
   ```
4. ПКМ компасом по рамке.
5. Вход в портал отправит игрока на указанный адрес.

---

## Команды

Корневая команда: `/portal`  
Алиасов нет (можно добавить на стороне сервера при желании).

| Команда | Описание | Право |
|---------|----------|-------|
| `/portal` | Справка по командам | `portalwarp.use` |
| `/portal warp create <название>` | Создать локальный портал (2 рамки) | `portalwarp.use` |
| `/portal connect create <название> <адрес>` | Создать серверный портал (1 рамка) | `portalwarp.admin` |
| `/portal delete <название>` | Удалить портал | `portalwarp.use` |
| `/portal list` | Список всех порталов | `portalwarp.use` |
| `/portal reload` | Перезагрузить конфиг и порталы | `portalwarp.admin` |

### Примеры

```text
/portal warp create spawn_to_shop
/portal connect create to_survival "survival.myserver.net:25565"
/portal delete spawn_to_shop
/portal list
/portal reload
```

> Для адресов с двоеточием (`ip:port`) аргумент лучше заключать в кавычки.

---

## Права (permissions)

| Право | По умолчанию | Описание |
|-------|--------------|----------|
| `portalwarp.use` | `true` (все игроки) | Базовые команды: создание варпов, список, удаление, справка |
| `portalwarp.admin` | `op` | Межсерверные порталы и `/portal reload` |

Пример выдачи через LuckPerms:

```text
/lp user <ник> permission set portalwarp.admin true
```

---

## Конфигурация

Файл: `plugins/PortalWarp/config.yml`

```yaml
# Cooldown after teleport in ticks (20 ticks = 1 second)
teleport-cooldown: 20

# Maximum portal interior size (blocks)
max-interior-size: 100

# Portal particle settings
particles:
  interval: 3
  count: 6
  spread: 0.4
  color:
    red: 60
    green: 140
    blue: 255
  size: 1.5

# Fill material and ambient effects
portal:
  fill-material: CYAN_STAINED_GLASS_PANE
  ambient-particle: ENCHANT
  ambient-count: 2

# Portals (managed automatically — do not edit by hand)
portals: []
```

### Параметры

| Ключ | Тип | Описание |
|------|-----|----------|
| `teleport-cooldown` | int | Кулдаун после ТП (тики) |
| `max-interior-size` | int | Максимум блоков внутри рамки |
| `particles.interval` | int | Интервал спавна частиц |
| `particles.count` | int | Количество частиц за тик/цикл |
| `particles.spread` | double | Разброс частиц |
| `particles.color.*` | int 0–255 | RGB цвет пыли |
| `particles.size` | double | Размер частицы |
| `portal.fill-material` | Material | Материал заполнения интерьера |
| `portal.ambient-particle` | Particle | Дополнительный ambient-эффект |
| `portal.ambient-count` | int | Количество ambient-частиц |
| `portals` | list | Данные порталов (автосохранение) |

После изменений выполните `/portal reload`.

---

## Как работает создание рамки

1. Игрок запускает команду создания и держит **возвратный компас**.
2. ПКМ по блоку рамки или по воздуху внутри неё.
3. `FrameAnalyzer` ищет замкнутый контур в одной из плоскостей.
4. Проверяется, что координаты не заняты другим порталом.
5. Интерьер заполняется материалом из конфига, сохраняются точки телепорта и ориентация.
6. Запускаются задачи частиц (совместимо с Folia).

Ограничения анализа (в коде): размер рамки и интерьера ограничены, чтобы не перегружать сервер при огромных постройках. Дополнительно действует `max-interior-size` из конфига.

---

## Сборка

### Клонирование

```bash
git clone https://github.com/Goblin_Thug/PortalWarp.git
cd PortalWarp
```

> Замените URL на фактический адрес вашего репозитория.

### Maven

```bash
mvn clean package
```

Готовый артефакт:

```text
target/PortalWarp-1.0.0.jar
```

(имя зависит от `finalName` и версии в `pom.xml`).

### Требования к окружению сборки

- JDK **25**
- Maven с доступом к репозиторию [PaperMC](https://repo.papermc.io/repository/maven-public/)

---

## Структура проекта

```text
PortalWarp/
├── pom.xml
├── README.md                 # Documentation (EN, primary)
├── README_RU.md              # Documentation (RU, additional)
├── LICENSE
├── CHANGELOG.md
├── CONTRIBUTING.md
├── SECURITY.md
├── .gitignore
├── .gitattributes
├── .editorconfig
└── src/main/
    ├── java/ru/goblin_thug/portalwarp/
    │   ├── PortalWarp.java
    │   ├── commands/CommandHandler.java
    │   ├── listeners/
    │   │   ├── CompassListener.java
    │   │   └── PortalListener.java
    │   ├── managers/
    │   │   ├── PortalManager.java
    │   │   └── Portal.java
    │   └── utils/
    │       ├── FrameAnalyzer.java
    │       ├── FrameData.java
    │       └── Utils.java
    └── resources/
        ├── paper-plugin.yml
        └── config.yml
```

---

## Совместимость и заметки

- Плагин рассчитан на современный Paper plugin loader (`paper-plugin.yml`).
- `folia-supported: true` — используйте региональные задачи; не переносите логику на устаревшие глобальные планировщики без адаптации.
- Секция `portals` в конфиге **управляется плагином**. Ручное редактирование может привести к ошибкам загрузки.
- Межсерверный переход зависит от клиента/прокси (Vanilla transfer / ваш сетевой стек). Убедитесь, что адрес доступен игрокам.

---

## Участие в разработке

Читайте [CONTRIBUTING.md](CONTRIBUTING.md).  
Ошибки безопасности — по [SECURITY.md](SECURITY.md).

---

## Лицензия

Распространяется под лицензией **MIT**.  
Copyright (c) 2026 **Goblin_Thug**.

Полный текст: [LICENSE](LICENSE).

---

## Автор

**Goblin_Thug**  
Версия плагина: **1.0.0**

Если проект вам полезен — поставьте ★ на GitHub и расскажите о багах через Issues.
