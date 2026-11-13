# API аддонов — v1

Введено в **IC 2 Classic Re 0.65.0**, Minecraft **1.21.1**, NeoForge **21.1.219**, Java **21**. Это API данного порта, не бинарно совместимое с API оригинального IC2 Classic 1.19.2.

Публичный контракт: `ic2.api`, кроме `ic2.api.internal`. Классы `ic2.core` — реализация: не используйте их в аддонах. При несовместимом изменении публичного контракта будет увеличена версия API. Проверяйте её методом `Ic2Api.version()`: ссылку на константу Java может встроить в байткод аддона.

## Подключение

Отдельный API-артефакт и Maven-репозиторий пока не публикуются. API находится в обычном JAR мода. Добавьте полученный JAR в `libs` проекта аддона:

```groovy
dependencies {
    compileOnly files('libs/ic2-classic-re-0.66.0.jar')
    runtimeOnly files('libs/ic2-classic-re-0.66.0.jar')
}
```

Не включайте классы IC2 в JAR аддона: мод должен загружаться отдельно. Используйте такую же версию Minecraft/NeoForge и Java toolchain 21.

В `META-INF/neoforge.mods.toml` аддона укажите зависимость, заменив `exampleaddon` своим mod ID:

```toml
[[dependencies.exampleaddon]]
modId = "ic2"
type = "required"
versionRange = "[0.65.0,0.67.0)"
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

Поддерживаются основные `MachineEntity` (включая накопители, обычные трансформаторы, реакторы и машины), продвинутые машины, солнечная турбина/шлаковый генератор, устройства энергетической/плазменной ветки и два бака. Запрашивайте **контроллер**: корпуса, клапаны, реакторные камеры и планировочные вспомогательные сущности этот интерфейс не предоставляют.

`active` отражает блочное свойство `lit`, если оно есть; иначе `false`. Это индикатор блока, не универсальный признак наличия энергии. Состояние структуры `formed` отражает последнюю проверку контроллера и может отставать до следующего тика. Снимки не передаются клиенту автоматически: сетевую синхронизацию своего интерфейса выполняет аддон.

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
