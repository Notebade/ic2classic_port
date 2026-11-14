# API аддонов — v1

Введено в **IC 2 Classic Re 0.65.0**, Minecraft **1.21.1**, NeoForge **21.1.219**, Java **21**. Это API данного порта, не бинарно совместимое с API оригинального IC2 Classic 1.19.2.

Публичный контракт: `ic2.api`, кроме `ic2.api.internal`. Классы `ic2.core` — реализация: не используйте их в аддонах. При несовместимом изменении публичного контракта будет увеличена версия API. Проверяйте её методом `Ic2Api.version()`: ссылку на константу Java может встроить в байткод аддона.

## Подключение

Отдельный API-артефакт и Maven-репозиторий пока не публикуются. API находится в обычном JAR мода. Добавьте полученный JAR в `libs` проекта аддона:

```groovy
dependencies {
    compileOnly files('libs/ic2-classic-re-0.71.0.jar')
    runtimeOnly files('libs/ic2-classic-re-0.71.0.jar')
}
```

Не включайте классы IC2 в JAR аддона: мод должен загружаться отдельно. Используйте такую же версию Minecraft/NeoForge и Java toolchain 21.

В `META-INF/neoforge.mods.toml` аддона укажите зависимость, заменив `exampleaddon` своим mod ID:

```toml
[[dependencies.exampleaddon]]
modId = "ic2"
type = "required"
versionRange = "[0.71.0,0.72.0)"
ordering = "AFTER"
side = "BOTH"
```

Диапазон здесь закреплён на проверенной версии порта. Расширяйте его после проверки аддона с новыми релизами; версия API и версия мода — разные числа.

## Поиск содержимого

```java
import ic2.api.Ic2Api;
import net.minecraft.resources.ResourceLocation;

var id = ResourceLocation.fromNamespaceAndPath("ic2", "plasmafier");
var block = Ic2Api.block(id);       // Optional<Block>
var item = Ic2Api.item(id);         // Optional<Item>
var steam = Ic2Api.fluid(ResourceLocation.parse("ic2:steam"));
var itemIds = Ic2Api.itemIds();     // неизменяемый отсортированный список ID ic2
var blockIds = Ic2Api.blockIds();
var fluidIds = Ic2Api.fluidIds();
```

Поиск по ID допускает любой namespace. Неизвестный ID возвращает `Optional.empty()`, а не запасной блок воздуха/пустую жидкость. Списки содержат только namespace `ic2`. Вызывайте после регистрации содержимого: в `FMLCommonSetupEvent.enqueueWork` или позже. Не разрешайте ID в статических инициализаторах аддона до загрузки реестров.

Предметы/блоки/жидкости самого аддона регистрируются обычными `DeferredRegister` NeoForge под namespace аддона. API IC2 не заменяет эти реестры.

## Чтение машин

Дополнения текущего переноса сохраняют версию API **1**. Зарядные плиты возвращают фактические значения ёмкости и входного пакета с учётом улучшений, а не базовые характеристики.

| Семейство | Дополнительные ключи |
|---|---|
| Зарядные плиты | `packet_eu` — входной пакет; `transfer_limit_eu` — бюджет зарядки/разрядки за тик; `installed_upgrades` — битовая маска проекционных улучшений; `range_milliblocks` — горизонтальное расширение AABB в тысячных блока; `active_ticks`, `cooldown_ticks`; `nuclear_ticks`, `nuclear_output_eu` — запас тиков и EU за тик текущего топлива делительной плиты. |
| Монитор | `screen_color` — ID красителя 0–15; `screen_scale` — настройка 1–10; `screen_alignment` — настройка выравнивания; `screen_width`, `screen_height` — размер стены; `screen_master` — 0/1. |
| Обогатитель урана | `enrichment_points`; `progress_units` / `progress_target` — прогресс обогащения слитка. Подготовка катализатора выполняется отдельно. |
| Магнетизатор, катушка Теслы | `input_packet_eu`; `upgraded` — 0/1; `redstone_disabled` — 0/1. |
| Загрузчик чанков | `radius` — 0–3; `chunk_processing` — 0/1; `cost_eu` — EU за тик; `access_mode` — 0: публичный, 1: владелец и союзники команды, 2: владелец; `forced_chunks` — размер активной области. Оператор имеет доступ во всех режимах; изменение настроек доступно владельцу/оператору. |
| Колоссальные машины | `formed`, `structure_edge`, `recipe_lanes` — 2/4/8 линий в кубах с ребром 2/3/4; `water_mb`, `steam_mb`; `progress_target` — 6000; `lane_0_progress` … `lane_7_progress`; `processing` — 0/1. `energyCapacity` учитывает установленные улучшения. |
| Электрический зачарователь | `mode` — 0: зачарование, 1: объединение с зачарованными книгами, 2: перенос первых чар на книгу; `stored_xp`, `required_xp` — **очки XP**, не уровни; `enchant_level` — 0–30; `running`, `valid`; `progress_units`, `progress_target` — тики (100/100/250 по режимам). |
| Электрический рыбак | `formed`, `chain_length`, `fishing_luck` — длина цепи минус 4; `progress_units`, `progress_target` — тики, порог 300; `loot_rolls_per_cycle` — 32. |
| Паровой реактор | `output_steam_mb` — последний выход пара в мВ за цикл; старый `output_eu` сохранён для совместимости и у парового реактора тоже содержит мВ. |

Маска плиты: бит 0 — урон, 1 — разряд, 2 — соседние слоты хотбара, 3 — весь хотбар, 4 — приоритет брони, 5/6/7 — расширение поля на 1/2/3 блока. Дубликаты одного проекционного улучшения не складываются. Урон и разряд зависят от настройки PVP; разряд доступен начиная с HV. При разряде EU списываются и с предмета цели, и с плиты: это поведение оригинала.

## Улучшения машин

Предмет аддона может реализовать `ic2.api.machine.MachineUpgrade`. Контракт используется слотами зарядных плит, колоссальных и продвинутых машин. Проекционные слоты принимают отдельные улучшения плит. `Category` различает MACHINE, RECIPE, TRANSPORT, REDSTONE, AUDIO, PROCESSING и CUSTOM. Плиты принимают MACHINE/RECIPE; колоссальные и продвинутые машины отклоняют RECIPE; медленный измельчитель также отклоняет MACHINE, как в оригинале. Одного интерфейса недостаточно для установки в любое устройство.

Методы: `speedMultiplier`, `energyDemandMultiplier`, `extraEnergyStorage`, `storageMultiplier`, `extraTier`. Аргумент — установленный `ItemStack`; каждый модификатор применяется один раз на предмет в стопке. Дополнительная ёмкость сначала складывается, затем умножается; множители скорости и ёмкости перемножаются. Плита использует скорость для своего бюджета передачи; множитель расхода EU на рецепт у неё не используется. Tier ограничен 1–13, целочисленные значения насыщаются при переполнении. Методы должны быть чистыми и возвращать неотрицательные значения; обычный нейтральный множитель — 1.

`invertsRedstone` объединяется через OR, `togglesRedstoneSensitivity` — XOR один раз на установленную стопку; `soundMultiplier` перемножается на каждый предмет. Значение звука 0 отключает цикл машины. `tickUpgrade(stack, context)` вызывается сервером; результат true требует сохранения изменённой стопки. `recipeFinished(stack, context, result, xp)` сообщает о фактически завершённом выходе. Эти два обработчика подключены к продвинутым и колоссальным машинам; плиты используют пассивные модификаторы.

`UpgradeContext` предоставляет сервер, позицию, обработчики входов/выходов и необязательный обработчик жидкости. Контекст и его обработчики нельзя сохранять между вызовами. `connectionPositions()` возвращает доступные позиции подключения: контроллер либо все части колоссальной структуры через `ConnectionProvider`. Не загружайте чанки при работе улучшения. `DirectionalMachineUpgrade` читает/устанавливает необязательное направление стороны; пустое направление отключает встроенные простые импорт/экспорт.

Встроенные предметы: `upgrade_simple_import`, `upgrade_simple_export`, прежние `fluid_import_upgrade_simple`, `fluid_export_upgrade_simple`, `cobblegen_upgrade`, `upgrade_redstone_inverter`, `upgrade_redstone_sensitivity`, `upgrade_loudness`, `upgrade_muffler`, `upgrade_mute`. Старые ID жидкостных улучшений сохранены. Направление транспорта задаётся использованием предмета по грани блока; Shift выбирает обратную сторону.

## Пример чтения снимка

### Электрически зачаровываемые предметы аддона

Предмет, включая обычный незачаровываемый `Item`, может реализовать `ic2.api.item.ElectricEnchantable`. Метод `acceptsElectricEnchantment(stack, Holder<Enchantment>, boolean anvil)` определяет допустимые чары для этого предмета в электрическом зачарователе; `anvil` различает случайное зачарование/добавление книг и режим объединения. `electricEnchantability(stack)` по умолчанию равен 1, при расчёте ограничивается диапазоном 1–255. Методы должны быть чистыми. Случайный подбор дополнительно учитывает современные теги `IN_ENCHANTING_TABLE`, `CURSE`, `TREASURE` и совместимость чар; перенос уже существующих чар в режиме снятия не требует разрешения на их повторное применение.

Контракт проверяется настоящим предметом отдельного аддона: он не зачаровывается обычным способом, но получает только разрешённую аддоном эффективность в электрическом зачарователе. Отдельный предмет аддона также проверяет реальное применение `MachineUpgrade` в зарядной плите.

### Чтение снимка

Следующий пример относится ко всем поддерживаемым семействам.

```java
import ic2.api.Ic2Api;

// level: ServerLevel; pos: BlockPos; выполнение в серверном потоке
Ic2Api.machineInfo(level, pos).ifPresent(info -> {
    var blockId = info.blockId();
    boolean active = info.active();
    long stored = info.energyStored();
    long capacity = info.energyCapacity();
    var unit = info.energyUnit();  // EU, FE либо NONE
    var heat = info.values().get("heat"); // Long или null, если показателя нет
});
```

`MachineSnapshot` — неизменяемый снимок, не живая ссылка на машину. `values()` тоже неизменяема. Для новых значений запрашивайте новый снимок. Метод не загружает чанки и не формирует структуры. Для незагруженного чанка, обычного блока или неподдерживаемой сущности возвращается пустой `Optional`. Вызов вне серверного потока завершится `IllegalStateException`.

Поддерживаются основные `MachineEntity` (включая накопители, обычные трансформаторы, реакторы и машины), продвинутые и колоссальные машины, солнечная турбина/шлаковый генератор, устройства энергетической/плазменной ветки и два бака. Связанные энергетические корпуса/клапаны, части колоссальных машин и плавильни под давлением также разрешают снимок своего живого контроллера через `MachineSnapshot.Access`. Реакторные камеры и планировочные вспомогательные сущности пока отдельного доступа не предоставляют.

`active` обычно отражает блочное свойство `lit`; у загрузчика чанков — включённую область принудительной загрузки. Это индикатор работы, не универсальный признак наличия энергии. Состояние структуры `formed` отражает проверку контроллера; некоторые семейства проверяют живую геометрию при самом запросе, другие обновляют значение в тике. Снимки не передаются клиенту автоматически: сетевую синхронизацию своего интерфейса выполняет аддон.

Дополнительные показатели, если поддерживаются конкретным семейством:

| Ключ | Значение |
|---|---|
| `progress_units`, `progress_target` | Текущий прогресс и порог завершения. Это внутренние единицы, не обязательно тики; при пороге 0 процент не определён. |
| `heat`, `heat_capacity` | Внутренние единицы нагрева и предел, если доступен. Шкалы разных устройств различаются. |
| `processing` | 0/1: продвинутая машина обрабатывает рецепт. |
| `output_eu` | Последний рассчитанный выход ядерного реактора в EU за его цикл. |
| `output_eu_hundredths` | Выход дополнительного генератора в сотых EU/т. |
| `fuel_ticks` | Оставшиеся тики горения. |
| `speed_thousandths` | Скорость ротора мельницы, умноженная на 1000. |
| `free_air_permille` | Доля доступного воздуха мельницы в тысячных. |
| `fuel_max_ticks` | Длительность текущей заправки жидкотопливного генератора в тиках. |
| `fuel_units` | Внутренний запас топлива котла. Это не тики. |
| `fuel_mb`, `water_mb`, `steam_mb`, `lava_mb`, `plasma_mb` | Объём соответствующего запаса в мВ. `lava_mb` термояда включает резерв 10000 мВ. |
| `water_units` | Солнечная турбина: внутренние водяные единицы оригинала, две единицы на внешний мВ. |
| `uu_reserve` | Оставшийся резерв плазмафайера; единица расходуется на 1 мВ плазмы. |
| `material_units` | Материал термояда; один блок добавляет 1000 единиц. |
| `formed` | 0/1: последняя проверка многоблочной структуры. |
| `packet_eu`, `packets_per_tick` | Настройки регулируемого трансформатора. |
| `input_packet_eu` | Максимальный входной EU-пакет Flux Generator; его энергетический буфер измеряется в FE. |
| `fluid_mb`, `fluid_capacity_mb` | Наполнение и объём отдельного бака. Тип жидкости читайте через fluid capability. |

Отсутствующий ключ означает отсутствие показателя; не заменяйте его нулём без учёта смысла. Запас энергии и ёмкость всегда читаются вместе с `energyUnit`: например, у Flux Generator это **FE**, у котла/термояда — **NONE**.

Свой BlockEntity может реализовать `MachineSnapshot.Provider` и возвращать собственный снимок. `Ic2Api.machineInfo` распознает его автоматически. Набор дополнительных ключей аддона документируйте отдельно; неизменяемую карту создаёт конструктор `MachineSnapshot`.

Для связанных частей добавлен `MachineSnapshot.Access.resolveMachineInfo(): Optional<MachineSnapshot>`. Его используют части колоссальных машин, энергетических многоблоков и плавильни под давлением. Возвращается снимок **контроллера**, включая его `blockId`; при разрушенной структуре, удалённой/заменённой части или недоступном контроллере — пустой `Optional`. Повторное чтение проверяет живую связь; запрос не загружает чанки и не формирует структуру. Аддон может реализовать тот же контракт, соблюдая эти ограничения. При реализации обоих интерфейсов приоритет имеет `Provider`.

## Рецепты аддонов

Пока доступны программные дополнения для **компрессора** и **экстрактора**. Они автоматически работают также в сингулярном компрессоре/центрифужном экстракторе и попадают в соответствующие списки JEI при их построении.

```java
import ic2.api.recipe.Ic2Recipes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

private void setup(FMLCommonSetupEvent event) {
    event.enqueueWork(() -> {
        Ic2Recipes.addCompression(
            ResourceLocation.parse("exampleaddon:deepslate"),
            Items.COBBLED_DEEPSLATE, 1, Items.DEEPSLATE);

        Ic2Recipes.addExtraction(
            ResourceLocation.parse("exampleaddon:moss_slime"),
            Items.MOSS_BLOCK, Items.SLIME_BALL, 2);
    });
}
```

Полный пример: [ExampleAddon.java](src/addonExample/java/example/ExampleAddon.java). `check` отдельно компилирует его как потребителя API; пример **не входит** в JAR IC2. Собственные `DeferredItem` разрешайте через `.get()` внутри `enqueueWork`.

Правила регистрации:

* Регистрируйте в `FMLCommonSetupEvent.enqueueWork`. После `FMLLoadCompleteEvent` реестр закрывается; поздняя регистрация вызывает `IllegalStateException`.
* ID обязан быть уникальным среди всех программных дополнений. Используйте namespace аддона; `ic2` и `minecraft` запрещены.
* Один входной предмет на процесс допускает одно дополнение. Дубли ID/входа вызывают `IllegalArgumentException`; существующая запись не перезаписывается.
* Встроенные рецепты, включая совпадения по тегам, имеют приоритет. Этот API добавляет рецепты, не заменяет встроенные.
* Компрессор принимает заданное количество входа и выдаёт **один** предмет. Экстрактор потребляет **один** вход и выдаёт заданное количество. Количества должны помещаться в стандартный стак соответствующего предмета; воздух запрещён.
* Ингредиенты здесь — конкретные предметы, выходы — с компонентами по умолчанию. Теги, дополнительные компоненты, собственная длительность и EU-стоимость для этих дополнений пока не задаются. Используются параметры выбранной машины.

Чтение эффективных рецептов:

```java
import ic2.api.recipe.Ic2Recipes;
import ic2.api.recipe.ProcessingRecipe.Process;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

var recipe = Ic2Recipes.find(Process.COMPRESSION, new ItemStack(Items.COPPER_INGOT));
var all = Ic2Recipes.recipes(Process.EXTRACTION);   // неизменяемый снимок
var addons = Ic2Recipes.additions(Process.EXTRACTION);
```

`find` возвращает рецепт даже при недостаточном количестве входа: сравните размер стака с `inputCount()` самостоятельно. `recipes` перебирает зарегистрированные предметы, включая применимые рецепты тегов; вызывайте после загрузки тегов мира в серверном потоке. Не выполняйте полный перебор каждый тик. `additions` возвращает только программно добавленные записи. У встроенных рецептов ID синтетические: `ic2:builtin/<process>/<input namespace>/<input path>`; это идентификаторы просмотра, не имена JSON-файлов.

Для дробления, обычного крафта, плавки/обжига/копчения используйте существующие JSON-рецепты и data packs. Остальные программные реестры, события переработки, расширение ядерных компонентов и произвольные генераторные топлива в v1 пока не открыты.

## EU для собственных машин

BlockEntity аддона может реализовать `ic2.api.energy.EnergySink`:

```java
@Override
public boolean canReceiveEnergy(Direction side, int sourceStorageOutput, int packet) {
    return packet <= 128 && storedEu < capacityEu;
}

@Override
public int receiveEnergy(int available) {
    int accepted = Math.max(0, Math.min(available, capacityEu - storedEu));
    storedEu += accepted;
    if (accepted > 0) setChanged();
    return accepted;
}
```

`side` — грань получателя; `packet` — исходный пакет до потерь кабеля. `sourceStorageOutput` равен нулю для генераторов/трансформаторов либо номинальному выходу исходного накопителя. Проверка `canReceiveEnergy` не должна менять состояние. `receiveEnergy` выполняет приём и обязана вернуть число в пределах `[0, available]`; сохранение запаса в NBT обеспечивает сам аддон.

Для визуального соединения кабелей класс **Block** аддона дополнительно реализует маркер `EnergyConnectable`. Он не заменяет `EnergySink` у BlockEntity.

Отправка из собственного генератора в его серверном тике:

```java
import ic2.api.energy.EuNetwork;
import java.util.EnumSet;

int offered = Math.min(storedEu, 128);
int spent = EuNetwork.send(serverLevel, worldPosition, offered,
    EnumSet.allOf(Direction.class), 0);
storedEu -= spent;
if (spent > 0) setChanged();
```

`send` выполняет один пакет и возвращает **расход источника, включая потери кабелей**. Не уменьшайте заряд второй раз по значению, принятому получателем. Пакет и номинальный выход накопителя должны быть в диапазоне 0–32768 EU; для собственного накопителя передайте его номинальный выход последним аргументом. Метод работает только в серверном потоке, не загружает чанки и не содержит планировщика. Вызывать его следует из существующей загруженной машины, выдавшей разрешение на этот расход. Симуляции и отката нет. Лимит пакетов за тик и доступный запас контролирует аддон.

## Предметы, жидкости и FE

### EU-порты на обычных блоках многоблока

Загруженный контроллер может реализовать `EnergyPortOwner` (наследует `EnergySink`) и методом `ownsEnergyPort(BlockPos)` проверять живую принадлежность позиции своей структуре. Проверка не должна загружать чанки, перестраивать структуру или менять мир.

`EuNetwork.registerPorts(serverLevel, controllerPos, collectionOfPortPositions)` атомарно заменяет набор портов контроллера и возвращает `boolean`. Все позиции должны быть загружены и подтверждены контроллером; действующий чужой порт занять нельзя. Отказ не удаляет ранее зарегистрированный набор. `unregisterPorts(level, controllerPos)` снимает только порты этого владельца. `portOwner(level, portPos)` возвращает `Optional<BlockPos>` живого владельца. Все методы требуют серверный поток и не загружают чанки.

Регистрация — производное состояние: аддон восстанавливает порты после загрузки/проверки структуры и снимает при разрушении/удалении контроллера. Каждый EU-пакет повторно проверяет живую связь; незагруженный или заменённый контроллер не получает EU. Если владелец реализует `MachineSnapshot.Provider`, `Ic2Api.machineInfo` по обычному блоку порта возвращает его снимок. Это используется у деревянных колец электрического рыбака и проверяется настоящей передачей EU через обычные доски/брёвна. Обычный BlockEntity с собственным `EnergySink` имеет приоритет перед виртуальным портом.

### Стандартные capability

Для инвентарей, баков и FE используйте стандартные NeoForge capabilities:

```java
var items = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, side);
var fluids = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, side);
var fe = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, side);
```

Учитывайте `null`, стороны, режим simulate/execute и инвалидирование capability. Не меняйте возвращённые стаки в обход handler. EU и FE — разные системы: наличие EU-буфера не означает FE-capability. Flux Generator выдаёт FE только через предусмотренную выходную грань, конверсия односторонняя 4 FE/EU. У незавершённых структур операции через части блокируются.

## Проверки и статус

`gradlew.bat check --offline --console=plain -q` включает тесты жизненного цикла реестра и компиляцию отдельного примера аддона. Снимки возвращаются пятью семействами сущностей мода; реальные компрессоры/экстракторы читают дополнения API. Отдельный тестовый аддон проверен на выделенном сервере: добавление обоих рецептов, приоритет встроенного рецепта, закрытие реестра, неизменяемость снимка, отсутствие загрузки чанков и передача EU настоящему плазмафайеру. Длительная обработка, GUI/JEI и клиентская сетевая синхронизация ещё требуют проверки. Отдельный SDK, HTTP API, сетевой RPC и универсальная запись состояния машин в v1 не предоставляются.

Повтор серверной проверки:

```powershell
.\scripts\Test-AddonApi.ps1
```

Скрипт компилирует и упаковывает только временный тестовый аддон. Основной JAR IC2 не собирает. Использует отдельный мир runs/addon-api-check, слушает только 127.0.0.1 на временном порту, после проверок останавливает сервер и удаляет тестовый JAR. Результат — в runs/addon-api-check/logs/latest.log. Клиентские assets при этом не скачиваются. Проверка включается свойством ic2.api.verify; не включайте его с примером аддона в обычном игровом мире.

В 0.66.0 API v1 дополнен данными мельницы и жидкотопливного генератора. У мельницы output_eu_hundredths означает потенциальный выход при текущей скорости, у жидкотопливного генератора — выработку последнего тика (0 при паузе). Скрипт серверной проверки также проверяет новые генераторы, капсулы и сохранение их состояния.

В 0.67.0 через тот же API v1 читаются газификаторы, рефайнер и бочка. Дополнительные ключи: gas_mb, input_1_mb, input_2_mb, output_mb (объёмы в мВ); age_ticks (прогресс текущего года), age_stage (годы), whisky_grade (0/1/2/3/4/5 для молодого/10/12/15/25/50-летнего), grist_units и opened (0/1). Каталог рефайнера пока не является открытым программным реестром API.

В 0.68.0 тот же API v1 видит зарядные столы, батарейные станции, площадки и перенаправители. Новые ключи: packet_eu, sorted (0/1), packets_per_tick (станции: 4), active_ticks, cooldown_ticks, pending_packets, connected_sides и share_down/up/north/south/west/east (проценты). connected_sides — битовая маска D/U/N/S/W/E с битами 0–5; имеет смысл у главного узла. energyStored ведомого включает очередь и остаток; у главного это остаток до 5 EU. Столы и площадки реализуют публичный EnergySink, станции и ведомые не принимают EU из сети. Дополнительных точек регистрации рецептов или изменяемого доступа к инвентарям в API v1 не вводится. [Правила работы устройств](docs/energy-redirectors-and-charging.md).

В 0.69.0 API v1 дополнен телеметрией планировщика: simulation_cycles, total_production (EU либо мВ пара по режиму), fuel_runtime_ticks, fuel_remaining_ticks (верхняя оценка), efficiency_permille (электрический КПД ×1000; −1 для парового режима/нет базовой выработки), full_run_state (0 — обычный, 1 — полный в работе/паузе, 2 — топливо закончилось, 3 — перегрев). Монитор публикует cards и requires_redstone; светильники — brightness. Трубы публикуют fluid_mb, capacity_mb и transfer_mb (мВ/тик трубы-источника), с EnergyUnit.NONE. Обычный valve перенаправляет capabilities сформированного контроллера, как прежние специализированные клапаны. Контракт и версия API не изменены. [Поведение и границы реализации](docs/nuclear-and-generation-followup.md).

0.70.0: базовые машинный буфер и личный сундук публикуют storage_slots и locked; буфер также сообщает EU/ёмкость. Публичный контракт остаётся v1. Регулируемая генерация отражается в фактических данных машин. Классы интеграционных проверок, включая прямую проверку внутреннего ReactorExplosion, не входят в стабильный API аддонов.

### EU cable hazards

`EuNetwork.send` includes real conductor breakdown and nearby electric injury for energy actually accepted by a sink. An offered packet with no demand does not burn wires or shock entities. Per-path exposure takes the largest exposed-cable value; packets accumulate until the next level tick, then deal ceil(exposure/64) electricity damage. Creative/spectator/dead entities and full hazmat suits are excluded. The return value still includes actual delivered EU plus cable losses; callers must debit it exactly once. Cable traversal and side effects do not load chunks.

### Prepared world-fluid pumps

A block entity implementing `ic2.api.machine.FluidPump` accepts a complete `FluidStack` through `pumpFluid(fluid, simulate)`. Native basic and accelerated pumps require their charged cycle and sufficient tank room. A successful simulation leaves state unchanged; successful execution fills the tank and resets progress. It never removes the world fluid block: the miner must validate/remove its source after acceptance. Call only on the server thread. Unsupported native machines return false; removed pump instances reject calls. Accelerated-pump telemetry: input_packet_eu=128, progress_units/target=25, fluid_mb; capacity=2500 EU.

### Mining tool contracts

`ic2.api.item.MiningDrill` owns `canUse`, `canMine`, `consumeUse`, `progressPerTick` and `machineEuPerTick`. The drill item accounts for its own charge/durability; the controller accounts for machine EU separately. `WorldScanner.scanRadius(stack, useEnergy)` is a free range query with false; true spends one scan or returns zero. `isValuable` checks loaded block data, and `scansFluids()` distinguishes fluid scanners. Methods must not load chunks. These contracts support the ongoing native miner/ranged-pump dependency transfer; their controllers are not yet marked complete.

### Native miner

The miner now consumes the public MiningDrill/WorldScanner contracts, including real items from a separately compiled addon. Inventory roles: 0 battery, 1 non-fluid scanner, 2 pipes/backfill blocks, 3 drill. Addon drill progress and machine EU are bounded at the controller boundary; tool-owned consumption is a separate completed-use callback. Native machine telemetry: input_packet_eu=128, progress_units/target=200, blocked, queued_targets, pipe_tip_y; capacity=10000 EU. Cached item access rejects a removed controller. Break protection is checked before working and before world mutation. Adjacent prepared FluidPump implementations enable source-fluid mining.

### Native ranged pump

`ranged_pump` accepts WorldScanner items with scansFluids=true. Slots: 0 battery, 1 pipes/backfill, 2 scanner. Its snapshot reports input_packet_eu=128, progress_units/target=5, fluid_mb, queued_targets, searching and pipe_tip_y; capacity=5000 EU. The tank is extraction-only and DOWN fluid/item access is disabled. Search uses up to 32 positions per controller tick and loaded chunks only. Target/queue/progress persist; an incomplete derived search restarts after reload. The source fisher acquisition recipe is now present with its real ranged-pump dependency.

### Optional overvoltage behavior

Existing `EnergySink` implementations keep their packet-rejection semantics. An addon may instead implement `ic2.api.energy.OvervoltageHandler`. Its positive-packet acceptance must be monotone: an accepted one-EU probe establishes an active input; rejection of the actual larger packet invokes `onOvervoltage(side, packet)` instead of receipt. This contract receives packet sizes after cable loss. The addon owns its fuse/damage behavior; IC2 does not destroy addon blocks automatically and records zero EU received for the overload callback. Native sinks implement this contract with source electrical explosions. A full/disabled/wrong-side sink that rejects one EU receives no overload callback. Both paths are verified by actual registered blocks of the separately compiled addon.

### EU item storage

`ic2.api.item.ElectricItem` can be implemented by a plain addon Item. Implement capacity(), transferRate(), minimumPacket(), charge(stack), setCharge(stack, value) and optionally canProvideEnergy() (default true). minimumPacket is the required machine tier expressed as 32/128/512/etc. EU; it is not the minimum quantity received per transfer. Queries must be pure; capacity must be positive, rate nonnegative and charge within [0, capacity]. setCharge changes only the supplied stack's charge and preserves its count. ElectricItem.supports validates these boundaries and charging/provider eligibility. Native rechargeable items implement this interface; native chargers/battery slots, portable/disposable charging and machine battery/tool charging consume it. Existing RechargeableItem.supports remains a compatible delegate.

A real addon stores EU in its own CustomData and is charged by both a held native battery and a BatBox with exact balance checks. FE converter capabilities also resolve live ownership: cached access returns zero after removal, including extraction simulation and capacity queries. Existing snapshots retain FE units for converters, EU for transformers/storage.

### Source EU distribution

One send first discovers shortest loaded routes and active receivers. Linked native parts and plain-block ports represent one controller, retaining its cheapest active input. Gross EU is shared using inverse path loss (minimum weight loss 0.1 EU); integer path losses are charged per executed allocation round. Remaining energy is redistributed after demand fills. Voltage-sensitive inputs see their allocated share after cable loss, so a 512-EU source split into four 127-EU delivered branches does not falsely overvolt MV receivers. The returned debit remains actual received EU plus executed path losses; overload callbacks still record no receipt. Ordinary legacy addon sinks keep the original offered-packet acceptance argument.

### Final 0.71.0 details

recipeFinished receives a detached result or ItemStack.EMPTY for chance-failed/partial output; rare-earth partial and completed batches both invoke it. Power-structure reads and fluid simulation require existing bound ownership and cannot repair replacement parts. Native cable fluid state is exposed through BlockState.getFluidState and the standard LiquidBlockContainer/BucketPickup interfaces (serialized fluid=none/water/lava). Readonly API calls and simulations continue to avoid chunk loading and structure formation. Complete stage scope and evidence: [energy/advanced completion](docs/energy-advanced-completion.md).

## Обычная экипировка (этап 8, в работе)

Через `Ic2Api.item(id)` доступны `tool_{axe,hoe,pickaxe,shovel,sword}_{bronze,aluminium}`, `armor_{bronze,composite}_{helmet,chest,leggings,boots}` и `{bronze,aluminium,composite,nano}_shield`. Обычные предметы используют стандартные контракты Minecraft `TieredItem`, `ArmorItem`, `ShieldItem`, ремонт и теги инструментов/брони. Нанощит реализует `ic2.api.item.ElectricItem`: 100000 EU, 300 EU/передачу, пакет от 128 EU; отдача энергии запрещена, блокирование требует 50 EU, износ расходует 50 EU за единицу. Заряд изменяйте через интерфейс, без зависимости от `ic2.core` или внутреннего компонента. API остаётся v1. Остальная экипировка и пункт 7 ещё переносятся.

### Режимы инструментов

`ic2.api.item.ConfigurableTool`: `modes()` перечисляет устойчивые строковые значения; `mode(stack)` читает режим; `setMode(stack, mode)` возвращает false для чужого предмета/неизвестного режима без изменения данных. Меняйте на сервере. Для `diamond_drill`/`rock_cutter_drill`: normal/dirt; для `advanced_drill`: single/area. Режим сохраняется с ItemStack, переключение — Shift+ПКМ. Все три варианта реализуют MiningDrill, ElectricItem и ElectricEnchantable. В шахтёре MiningDrill выполняет одиночную операцию без разрушения 3×3.

Рецепты `ic2:sized_shaped` используют стандартные поля shaped-рецепта плюс `counts`: объект `{ "minecraft:end_crystal": 3 }`, количества 1–64 по item ID. Проверяется каждая ячейка; форма, отражение, результат и компоненты стандартные. Указанные количества списываются в верстаке и ванильном Crafter. API остаётся v1.

### Мотыги и бензопилы

`electric_hoe`, `advanced_hoe`, `chainsaw`, `advanced_chainsaw` доступны через Ic2Api.item и ElectricItem, отдача EU запрещена. Мотыги расходуют 50/100 EU за изменённый блок; улучшенная хранит 100000 EU, принимает пакеты 512 EU и через ConfigurableTool предоставляет радиусы "0".."5". Обычная хранит 10000 EU/32 EU. Бензопилы хранят 10000 EU, принимают 32/512 EU и расходуют 50/100 EU за операцию; стрижка IShearable требует две операции. Действия инструментов используют стандартные NeoForge ItemAbilities/BlockToolModificationEvent, валка — стандартный BreakEvent. Прочие части экипировки пока переносятся.

### Ящики инструментов

`ic2.api.item.Toolbox` у предметов `toolbox`/`carbon_box`/`iridium_box`: `slots()` = 8/15/45; `accepts(stack)` проверяет вставку; `stored(box, slot)` возвращает копию либо EMPTY; `store(box, slot, item)` копирует данные, неверная вставка возвращает false без изменения. Пишите на серверном потоке. Пустой стек очищает слот. Старое содержимое сохраняется, даже если новый фильтр его не принимает.

`scroll(box, player, direction)` на сервере меняет удерживаемый инструмент с непустой ячейкой карбонового/квантового ящика; direction задаёт знак шага. Проверяет владение конкретным стеком ящика и допустимость вкладываемого инструмента; копия чужого/собственного ящика прав на инвентарь не даёт. В игре Shift+ПКМ ящиком обменивает инструмент в другой руке. Базовый ящик прокрутку не поддерживает.

Для инструментов аддона: реализуйте `BoxableItem.canStoreInToolbox(stack)` либо добавьте item ID в тег `ic2:toolbox_tools`. Обычные TieredItem/ShearsItem/FishingRodItem/FlintAndSteelItem/BrushItem и ElectricItem уже принимаются. Вложенные ящики и предметы с запретом контейнеров не принимаются. Нативные рецепты `ic2:toolbox_upgrade` сохраняют DataComponents.CONTAINER при увеличении ёмкости. Диагностика ящиков ещё переносится.

### Газонокосилка

`mowing_tool` реализует ElectricItem (10000 EU, 100 EU/передачу, пакет 32 EU, без отдачи), ElectricEnchantable и ConfigurableTool с off/on. ПКМ переключает; работа в основной/второй руке раз в пять тиков обрабатывает 3×3×3 вокруг выбранного блока. CropBlock исключён, защита BreakEvent соблюдается, дроп идёт в инвентарь с обычным выбросом остатка при переполнении. Расход: 5 EU за убранный блок, 1 EU за ручную добычу ненулевой твёрдости/успешное попадание по существу. Поведение AXE_SCRAPE сохранено. Нативные события дропа и публичные режимы доступны аддонам.

### Питание от экипировки и расход EU

`ElectricEquipment.energyCost(stack, base)` применяет исходные +15%/уровень Efficiency, затем −10%/уровень Unbreaking с усечением после каждого шага. base должен быть неотрицательным; результат не уходит в отрицательные значения. Указанные выше базовые расходы инструментов относятся к предметам без зачарований.

`canUse(stack, base)` — чистая проверка, без зарядки/изменения инвентаря. `use(stack, base, wearer)` на сервере пополняет предмет от надетых поставщиков EU, списывает полный скорректированный расход и снова пополняет; false не уничтожает недостаточный остаток заряда. wearer может быть null для машинной операции без доступа к экипировке. `chargeFromArmor(target, wearer)` возвращает перенесённые EU; не трогает предметы в руках и не отбирает EU у canProvideEnergy=false. По оригиналу этот перенос игнорирует ограничения пакет/скорость зарядника. Для обычных зарядников по-прежнему применяются ElectricItem.minimumPacket/transferRate.

`batpack`/`lappack`/`quantum_pack` надеваются на грудь, `industrial_belt` на ноги. Ёмкости 60000/300000/1200000/30000 EU; скорости 100/250/1000/100; пакеты 32/128/512/32. Это ElectricItem-поставщики без пассивной брони. Зачарования, ремонт, обычная экипировка и данные используют стандартные контракты Minecraft/NeoForge.

`ic2:electric_shaped` принимает стандартные поля shaped-рецепта и переносит сумму EU ингредиентов в единственный электрический результат с ограничением по его ёмкости. `ic2:sized_shaped` теперь переносит EU так же. Копия результата и ингредиенты не делят изменяемое состояние. Несколько батарей в результате разбора пояса остаются незаряженными, как в исходном рецепте. Пакеты/расходы API остаются в EU, API v1.

### Генерация в экипировку

`ElectricEquipment.offerToArmor(wearer, offered)` на сервере распределяет сгенерированные EU по надетым электрическим предметам, соблюдая transferRate и остаток ёмкости каждого. Возвращает принятые EU; отрицательное предложение запрещено. От chargeFromArmor отличается направлением и соблюдением скорости приёма. Не заряжает предметы в руках.

`solar_helmet`/`advanced_solar_helmet` дают 1/5 EU за тик игрока при исходной видимости солнца: измерение с небесным светом, день, открытое небо и отсутствие дождя/грозы в биомах с осадками. Это обычная броня с одним очком защиты, без собственной EU-ёмкости. Проверяются фактические PlayerTickEvent и обновления освещения. Для крафтов экипировки опубликованы теги c:ingots/refined_iron и c:rubber; все используемые крафтами теги проверяются на разрешение в непустые ингредиенты.

### Тики и буферы генерации брони

`ArmorTickItem.tickArmor(stack, wearer, slot)` автоматически вызывается один раз за серверный PlayerTickEvent.Post для надетого одиночного стека живого игрока. Порядок: ноги/ботинки FEET, поножи LEGS, нагрудник CHEST, шлем HEAD. Аддону достаточно реализовать интерфейс; отдельный обработчик регистрации не требуется. Клиентский GUI/отрисовка этим контрактом не вызываются.

`static_boots` реализуют GeneratedArmor, а не ElectricItem: generatedEnergy(stack), generationCapacity() и setGeneratedEnergy(stack, amount) дают доступ к буферу генерации (значения ограничены 0..22 EU). Исходный порог остановки — 20, последний прирост может дать 22. Обычная ходьба на пять блоков даёт 1 EU; прирост за проверку ограничен 3 EU, на шерстяном ковре действует исходный бонус. В воздухе, воде и при езде опорная позиция сбрасывается. Раз в десять мировых тиков буфер заряжает надетые накопители. Перенос и генерация в одном тике сохраняют энергию без повторного учёта перенесённого остатка. При записи через API используйте серверный поток; неверный тип стека отвергается.

### Параметры EU конкретного стека

ElectricItem теперь дополнительно предоставляет capacity(stack), transferRate(stack), minimumPacket(stack), canProvideEnergy(stack). По умолчанию они вызывают прежние методы без аргументов: существующие фиксированные предметы аддонов совместимы. Для модульных предметов переопределяйте варианты со стеком; charge/setCharge должны соблюдать ёмкость этого же стека. Получатели параметров должны использовать варианты со стеком, а не считать базовое значение свойством всех экземпляров предмета.

ElectricItem.supports, все нативные зарядники/слоты/машины, переносные батареи, ElectricEquipment и перенос EU в рецептах используют новые методы. Проверены разные ёмкости/скорости/напряжения/режимы поставщика у одного и того же Item отдельного аддона, включая нулевую базовую ёмкость. API остаётся v1; интерфейс дополнен методами по умолчанию, прежние сигнатуры сохранены.

### Modular equipment API (row 8 in progress)

`ArmorBattery` opts an electric item into armor battery slots through positive `armorTransferRate(stack)`. This rate may differ from portable charging; nonpositive values reject admission. Native energy/lapatron/glowtronic/UESC crystals use 250/1000/5000/10000.

`ArmorModule` declares a resource-ID category, compatible armor slots, pure `ArmorEnergyModifier` additions, capacity/rate multiplier bonuses and feature IDs. `sharesArmorEnergy` explicitly opts an electric module into transferring its stored EU to/from its parent. `installed`, `removed` and server `tickModule` callbacks run on committed operations. Native modifiers are cached per stack to avoid recursive parameter queries. Electrical modifiers are captured before installation callbacks and must not depend on mutable lifecycle state; change them through removal/reinstallation. Lifecycle callbacks may change operational module data, which is saved on the installed/returned stack. Live equipment changes belong on the server thread; tickModule is server-only. Default construction/recipe previews operate on fresh stacks.

`ModularArmor` exposes detached installed modules, category limits, features, default equipped construction and enhancement. A bare `new ItemStack(item)` is an empty shell. `defaultArmor()` returns a fresh source-default configuration. `canInstall` and simulated removal do not mutate inputs. Successful installation consumes one caller item. Removal returns the owned module with shared EU restored; an empty result means rejection (including insufficient remaining capacity). Enhancement downgrade rejects excess installed modules. Module order is stable and determines tick order. Queries expect the matching armor item, and mutations require a single armor stack.

`ProtectiveArmor` supplies a pure `(priority, ratio, maximum)` damage absorption descriptor and actual integer wear callback. Native processing runs before vanilla physical armor, sorts priorities descending and normalizes each priority group when total ratio exceeds one. The callback distinguishes absorbed damage from remaining vanilla armor wear. Return `Absorption.NONE` for excluded damage. `ic2:armor_magic` is an extensible damage-type tag excluded by native Nano protection. Vanilla hurt, enchantment and damage event processing remain in their native phases.

Native Nano IDs: `nano_suit_helmet`, `nano_suit_chestplate`, `nano_suit_leggings`, `nano_suit_boots`. `ic2:armor_configuration` stores category order and derived parameters, while `minecraft:container` stores module stacks. Both survive normal ItemStack serialization. `ic2:modular_armor` and `ic2:modular_armor_upgrade` are shaped recipe serializers for default construction and complete-state enhancement respectively.

Native passive modules: `air_refill_module` (GENERIC, HEAD), `charger_module` (CHARGER, all), `protective_module` (ANY, all), `extinguish_module` (ANY, all), `golden_module` (GENERIC, all). Golden exposes feature `ic2:golden` and native piglin neutrality. Air and effect-removal costs draw directly from parent EU, with no provider refill. `base_module` is an ordinary crafting ingredient. `ic2:batch_shapeless` uses ordinary shapeless fields with a single legal `result` and `output_count` 1–64; it returns a crafting batch, split into legal stacks by native menu transfer. Belt recovery deliberately returns uncharged batteries, matching source semantics.

`EquipmentInput(jump, boost, forward, hud, mode, alt)` transports intent, never authority to bypass equipment checks. `mask()` / `fromMask()` round-trip six bits; unknown bits throw. `EquipmentControls.update(player,input)`, `current(player)` and `clear(player)` are server-thread APIs; update rejects client/dead players, current returns NONE after twenty ticks without refresh or when time moves backwards. Native network handler accepts only the sending player's input and rejects unknown bits. Client sends changes and a ten-tick keepalive, with NONE while a screen is open.

`nightvision_goggles` implements ElectricItem, ArmorTickItem, ArmorModule and ConfigurableTool (`off`/`on`). Standalone uses 1 EU/tick with worn-provider refill; installed uses parent EU directly and keeps its own battery independent. Its mode is stored with the goggles module. Enabled vision removes blindness and refreshes 250 ticks night vision; turning off does not delete the existing effect. HUD+Mode toggles worn goggles with twenty-tick cooldown; Shift-use toggles held goggles. Gameplay key bindings: M mode, H HUD, left-control boost, left-alt alternate, plus vanilla jump/forward. GUI and HUD rendering remain in the separate graphics stage.

Movement modules: jump_boost_module (MOVEMENT/FEET), speed_boost_module (MOVEMENT/LEGS), step_assist_module (MOVEMENT/LEGS or FEET), parachute_module (BACK). Step advertises feature `ic2:step_assist`; native worn armor refresh applies one transient +0.6 STEP_HEIGHT bonus and revokes it on removal. Jump/Speed consume direct parent EU, with state stored on installed modules; parachute consumes none. Input uses EquipmentControls. `ic2-equipment-server.toml` setting `originalQuantumBehavior` defaults false, matching source. Flight-counter resetting is internal runtime integration, not a public addon permission API.

`water_walker_module` is MOVEMENT/FEET, consumes parent armor EU directly (100 every ten world ticks) while supporting downward movement at a liquid surface. Sneaking disables support; <100 EU rejects support. It uses source liquid detection, including lava, and does not itself grant lava damage immunity. `carbon_boat` item/entity is available through native item/entity registries, survives falls and ignores bubble columns, with ordinary entity persistence and custom boat-item drops.

`FallProtectiveArmor.absorbFall(stack,player,damage,simulate)` is invoked for a single worn FEET stack before native fall damage. Return paid whole damage points in [0,damage]; preview must not mutate. Native adapter accounts for SAFE_FALL_DISTANCE and FALL_DAMAGE_MULTIPLIER attributes, clamps addon return values and adjusts/cancels LivingFallEvent. Calls run on the server thread. Nano enhanced boots protect all affordable points; normal Nano and rubber boots cap long-fall reduction at eleven points, with actual EU/durability payment. Partial EU cannot buy free remaining protection. This intentionally fixes original unpaid/overstated fall reduction.

Nano protection recognizes existing `ic2:electricity` from native hazards. `ic2-equipment-server.toml`: `enableSpecialElectricArmor=true`, `electricSuitAbsorptionScale=1.0`, `electricSuitEnergyCostModifier=1.0` (last two 0.01–500). Special electrical absorption stores absorbed integer damage as EU when capacity is free; full armor spends normal wear unless electric boots stand above a nonempty block. Disabling special behavior uses normal affordable absorption. The source fall-payment rule remains separate from the general suit energy multiplier.

The native precision_wrench uses ElectricItem with 40,000 EU capacity, 350 EU transfer and a 128 EU packet limit. Rotation costs 100 EU and dismantling costs 1,000 EU before enchantment adjustments.
