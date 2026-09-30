# java_doom

![DOOM rodando em Java CLI com SDL2](screenshot/doom.png)

**Vídeo:** [DOOM rodando em Java — a 180 FPS](https://youtu.be/EbLxJNpGXcE)

DOOM generic portado de Harbour para **Java 17+ CLI + SDL2** (JNA). Não é aplicação web, nem Android, nem libGDX.

Por **Wagner Nunes da Silva**

- vagucs@bol.com.br
- vagucs@vagucs.com.br
- vagucs@gmail.com
- [www.vagucs.com.br](https://www.vagucs.com.br)
- [LinkedIn](https://www.linkedin.com/in/wagner-nunes-da-silva-b0a15360)

Esta árvore é um port de **[harbour_doom](https://github.com/vagucs/harbour_doom)** (`doom_hb`): o mesmo motor Chocolate Doom / doomgeneric que primeiro foi de C para Harbour, depois para Python (`doom_python`), PHP (`php_doom`) e Node (`node_doom`), agora de Harbour para Java.

Cada fonte leva o mesmo cabeçalho de autor dos `.prg` Harbour.

English version: [README.md](README.md)

---

## O que é este projeto

O motor do Chocolate Doom / doomgeneric foi traduzido para **Harbour** (`.prg` / `.ch`) com uma camada fina de C para Allegro 4.2.2. Esse trabalho está em [github.com/vagucs/harbour_doom](https://github.com/vagucs/harbour_doom). Este diretório é o **mesmo material de estudo**, em Java:

- Janela, teclas, PCM: **SDL2** via **JNA** (sem canvas AWT, sem navegador, sem HTTP)
- Framebuffer: 320×200, 8 bits PLAYPAL, `int[]`, escalado na janela
- Tic do jogo: 35 Hz (`TICRATE`), igual ao vanilla
- Renderer: BSP, visplanes, `R_DrawColumn` / `R_DrawSpan`, ponto fixo 16.16 `fixedMul` / `fixedDiv` (intermediários `long`)
- Mapa: VERTEXES, LINEDEFS, SIDEDEFS, SECTORS, SEGS, SSECTORS, NODES, THINGS, BLOCKMAP, REJECT
- Jogo: andar, portas, plataformas, interruptores, teleports, saída, itens, armas (incluindo motosserra subindo/cortando), barra de status, automap com Tab, som DS*, música MUS→MIDI, menu ESC (opções, load/save), totalização no intermission, wipe derretendo, bunny scroll, inimigos em look/chase/ataque

É necessário um IWAD legal (shareware `doom1.wad` ou comercial `doom.wad` / `doom2.wad` / etc.). Este repositório não distribui WAD comercial.

É um **port completo de jogabilidade** do motor Harbour para Java (os mesmos sistemas da árvore Node acabada, inclusive teleports, cheats extras, `P_ChangeSector` nas plataformas e Screen Size com `-/+`). A contagem de arquivos é condensada frente aos 100+ `.prg`; o comportamento segue as fontes Harbour.

O que não entra nesta árvore (o mesmo corte do `boot.prg` Harbour):

- Rede, música de CD, joystick
- Quantização de paleta `-colors` (experimento só no Harbour)
- Playback de demo, tabelas `info` vanilla completas

---

## Proposta educacional

Este projeto é, antes de tudo, um **material de estudo**. O DOOM (1993) é pequeno o bastante para ser lido de ponta a ponta e denso o bastante para ensinar engenharia de verdade: renderização por BSP, ponto fixo 16.16, laço por tics, sistema de arquivos WAD.

O port Harbour ensinou a ler C com os olhos de outra linguagem. O Python tirou o pré-processador e os arrays 1-based. PHP e Node tornaram a fronteira nativa **FFI / koffi para SDL2**. Este port Java mantém essa ideia e acrescenta mais uma lição: **`int` já é uma palavra de 32 bits em complemento de dois**, como no C. Wrap-around, `>>` / `>>>` e o produto 16.16 com `long` no meio mapeiam quase 1:1 — sem a armadilha do `intdiv()` no PHP, sem `BigInt` no JavaScript para `FixedMul`.

O outro lado da moeda: um produto que PHP e Node guardam como inteiro largo **estoura em Java** se for escrito como `int * int`. O `Collision.pointOnSide` (escolha do filho no BSP) precisa de `long`, senão o mapa desenha no lugar errado e uns passos parecem parede. É o mesmo overflow que o C resolvia com `FixedMul`.

O que o port pretende ensinar:

- **Seis linguagens, um motor.** C (`base_c/` no harbour_doom) → Harbour (`.prg`) → Python (`doom_python`) → PHP (`php_doom/src`) → TypeScript (`node_doom/src`) → Java (`java_doom/src/doom`). Os mesmos nomes (`P_Thrust`, `R_DrawColumn`, `A_Look`) para abrir as versões lado a lado.
- **O que os ponteiros faziam.** Java usa objetos e arrays; ângulos BAM e 16.16 ficam explícitos (`Compat.asU32`, `shar`, `fixedMul`) para as regras de overflow originais continuarem visíveis.
- **Onde uma VM basta.** O jogo inteiro roda na JVM. SDL2 só abre janela, lê teclado e enfileira PCM. JNA é a fronteira nativa, como o Allegro foi no Harbour e FFI/koffi no PHP/Node.
- **CLI, não servlet.** Não há canvas no navegador nem servidor de aplicação.
- **Modernização de legado.** Manter o comportamento idêntico, isolar a camada nativa, conferir contra o original.

Sugestão de roteiro:

1. Rode o jogo e leia `Doom.java` e `Game.java` — boot, tic, input.
2. Compare `Compat.java` com o Harbour `xhb_compat.prg` / `m_fixed.prg`, o PHP `Compat.php` e o Node `src/compat.ts`.
3. Abra `Renderer.java` ao lado de `r_main.prg` / `r_bsp.prg` / `r_segs.prg` / `r_draw.prg`.
4. Siga uma porta a partir do **Espaço** até `Specials.java`.
5. Siga um tiro a partir do **Ctrl** em `Player.java` até `Collision.lineAttack` e `Enemy`.
6. Leia `Collision.pointOnSide` — `long` vs `int` no produto do BSP.

---

## De C / Harbour / Python / PHP / Node para Java

Arrays em Java são 0-based, como C, Python, PHP e Node. Arrays Harbour eram 1-based; esse deslocamento some aqui.

A tabela abaixo é a mesma do port Node, com uma coluna a mais para Java:

| DOOM em C | Harbour | Python | PHP | Node | Java (esta árvore) |
|---|---|---|---|---|---|
| `struct` / `typedef struct` | `CLASS ... DATA` | `@dataclass` | `class` + propriedades tipadas | `class` + campos tipados | `class` + campos |
| `thing->x` | `thing:x` | `thing.x` | `$thing->x` | `thing.x` | `thing.x` |
| `NULL` | `NIL` | `None` | `null` | `null` | `null` |
| `array[0]` | `array[1]` | `array[0]` | `$array[0]` | `array[0]` | `array[0]` |
| `&`, `\|`, `^` | `hb_qbitAnd/Or/Xor` | `&`, `\|`, `^` | `&`, `\|`, `^` | `&`, `\|`, `^` | `&`, `\|`, `^` |
| `x >> n` sem sinal | `UShr(x, n)` | `ushr(x, n)` | `Compat::ushr($x, $n)` | `ushr(x, n)` | `Compat.ushr` / `>>>` |
| `x >> n` com sinal | `Shar(x, n)` | `shar(x, n)` | `Compat::shar($x, $n)` | `shar(x, n)` | `Compat.shar` / `>>` |
| estouro de 32 bits | `AsU32` / `AsInt32` | `as_u32` / `as_i32` | `Compat::asU32` / `asI32` | `asU32` / `asI32` | `int` já faz wrap |
| `fixed_t` 16.16 | `FixedMul` / `FixedDiv` | `fixed_mul` / `fixed_div` | `Compat::fixedMul` / `fixedDiv` | `fixedMul` / `fixedDiv` (BigInt) | `fixedMul` / `fixedDiv` (`long`) |
| framebuffer `byte *` | string Harbour | `bytearray` + LUT numpy | `array<int>` + SDL ARGB8888 | `Uint8Array` + SDL ARGB8888 | `int[]` + SDL ARGB8888 |
| Allegro 4.2.2 | GTALLEG / llibg | pygame | SDL2 via FFI | SDL2 via koffi | SDL2 via JNA |
| `Z_Malloc` | GC | GC | GC | GC | GC |
| globais `PUBLIC` | `PUBLIC` / `MEMVAR` | campos em `Game` | campos públicos em `Game` | campos públicos em `Game` | campos públicos em `Game` |
| 100+ arquivos `.prg` | 1:1 com o C | `doom/*.py` condensado | `src/*.php` condensado | `src/*.ts` condensado | `src/doom/*.java` condensado |

### Lado a lado: `P_Thrust`

C (`base_c/p_user.c` no harbour_doom):

```c
void P_Thrust (player_t* player, angle_t angle, fixed_t move)
{
    angle >>= ANGLETOFINESHIFT;
    player->mo->momx += FixedMul(move,finecosine[angle]);
    player->mo->momy += FixedMul(move,finesine[angle]);
}
```

Harbour (`p_user.prg`):

```harbour
PROCEDURE P_Thrust( player, angle, move )
    angle := UShr( angle, ANGLETOFINESHIFT )
    player:mo:momx += FixedMul( move, finecosine[ angle + 1 ] )
    player:mo:momy += FixedMul( move, finesine[ angle + 1 ] )
RETURN
```

Python (`doom/player.py`):

```python
def thrust(mo, angle, move):
    mo.momx += fixed_mul(move, fine_cos(angle))
    mo.momy += fixed_mul(move, fine_sin(angle))
```

PHP (`src/Player.php`):

```php
public static function thrust(Mobj $mo, int $angle, int $move): void
{
    $mo->momx += Compat::fixedMul($move, Tables::fineCos($angle));
    $mo->momy += Compat::fixedMul($move, Tables::fineSin($angle));
}
```

TypeScript (`src/player.ts`):

```typescript
static thrust(mo: Mobj, angle: number, move: number): void {
  mo.momx += fixedMul(move, fineCos(angle));
  mo.momy += fixedMul(move, fineSin(angle));
}
```

Java (`src/doom/Player.java`):

```java
public static void thrust(Mobj mo, int angle, int move)
{
    mo.momx += Compat.fixedMul(move, Tables.fineCos(angle));
    mo.momy += Compat.fixedMul(move, Tables.fineSin(angle));
}
```

`->` vira `.`. A indexação sem sinal do ângulo fica em `Tables.fineCos`. O `+ 1` do Harbour some. O `fixedMul` usa `long` para o produto 16.16 bater com o vanilla.

---

## O que continua nativo, e por quê

Java é **o motor do jogo**. Código nativo fica só onde a JVM não fala com a janela do SO ou com o mixer.

| Peça | API nativa | Por quê |
|---|---|---|
| `Video.java` | SDL2 via **JNA** | Janela, teclado, blit ARGB8888, fila PCM. Mesmo papel do Allegro no Harbour e FFI/koffi no PHP/Node |
| `Sound.java` | `javax.sound.midi` | Playback MUS→MIDI (`Mus2Mid.java` continua em Java). Sem MCI winmm |
| `lib/SDL2.dll` | SDL2 64 bits | Biblioteca drop-in; `SDL2_PATH` para sobrescrever |

O motor em si é Java comum. O `javac` gera as classes em `out/`.

---

## Tecnologia

| Camada | Este port | Harbour (`harbour_doom`) |
|---|---|---|
| Linguagem | Java 17+ (testado no Temurin 17.0.19) | Harbour / xHarbour |
| Janela, teclas, PCM | SDL2 (JNA 5.17) | Allegro 4.2.2 + GTALLEG |
| Blit da paleta / CRT | laço Java → `SDL_UpdateTexture` ARGB8888 | C em `doomgeneric_allegro.prg` |
| MIDI | Java Sound `Sequencer` | Allegro MIDI / MCI |
| IWAD | os mesmos lumps | os mesmos |
| Build | `javac` + `java` (ou `run.bat`) | `compile.bat` / `hbmk2` |

JDK necessário:

```
java -version    # 17+
javac -version   # 17+
```

SDL2: coloque **SDL2.dll** (64 bits) em `lib/` no Windows, ou defina `SDL2_PATH`. Dá para copiar o `php_doom/lib/SDL2.dll` (ou o do Node). Veja `lib/README.txt`.

JNA: `lib/jna-5.17.0.jar`. O `run.bat` baixa do Maven Central se o arquivo não estiver lá.

---

## Desempenho

Taxa típica de desenho no mesmo PC (320×200, janela, IWAD shareware). O jogo continua em 35 Hz (`TICRATE`); `-fps` mostra a taxa de blit. O Java cria o renderer SDL com `SDL_RENDERER_PRESENTVSYNC`, então o número trava no refresh do monitor (aqui **~180**, travado).

O mesmo comparativo do port Node, agora com Java: Harbour é interpretado na frente do Allegro, Python e PHP percorrem bytecode em cada coluna, Node é JIT com `BigInt` em cada `fixedMul`, Java é HotSpot com `int` / `long` primitivos. Por isso esta árvore fica acima do Node na mesma máquina.

| Port | FPS típico |
|---|---|
| Harbour (`doom_hb`) | ~12 |
| Python (`doom_python`) | ~8 |
| PHP (`php_doom`) | ~20 |
| Node (`node_doom`) | ~100 |
| Java (`java_doom`) | ~180 (travado no vsync) |

---

## Como rodar

JDK 17+, SDL2 no path (ou `java_doom/lib/SDL2.dll` no Windows). Neste diretório:

```
run.bat
run.bat -fps
run.bat -iwad ..\DOOM1.WAD -warp 1 1 -crt
```

Ou na mão:

```
javac -encoding UTF-8 -cp lib\jna-5.17.0.jar -d out src\doom\*.java
java -cp out;lib\jna-5.17.0.jar doom.Doom -iwad ..\DOOM1.WAD
```

O `run.bat` compila se faltar `out/`, copia o SDL2 de `php_doom/lib` ou `node_doom/lib` quando precisar, baixa o JNA se o jar não estiver lá, e se você omitir `-iwad` procura `DOOM1.WAD` aqui ou na pasta pai `doom_minimal`.

Linux: `sudo apt install libsdl2-2.0-0` e compile/rode com `:` no classpath.

macOS: `brew install sdl2` e compile/rode.

Sem `-iwad` procura um argumento `.wad`, depois `doom1.wad` / `DOOM1.WAD` / `doom.wad` / `doom2.wad` no diretório atual, `DOOMWADDIR`, esta pasta e a pasta pai `doom_minimal`.

---

## Teclas

Controles clássicos do DOOM (este port condensado; sem remap via `default.cfg`).

### Movimento e ações

| Tecla | Ação |
|---|---|
| Setas | Frente, trás, girar |
| **Shift** | Correr |
| **Alt** | Strafe (segurar) |
| **,** / **.** | Strafe esquerda / direita |
| **Ctrl** | Tiro (segurar repete; animação da arma + `A_ReFire`) |
| **Espaço** / **E** | Usar / abrir porta |
| **1** | Punho / motosserra (alterna) |
| **2**–**7** | Pistola, shotgun, chaingun, foguete, plasma, BFG |
| **Enter** | Começar a partir do título |
| **Tab** | Alterna o automap |

Movimento só com **setas** (sem WASD), para as letras ficarem livres para os cheats.

### Automap (com o mapa aberto)

| Tecla | Ação |
|---|---|
| Setas | Pan do mapa |
| **+** / **-** | Zoom |
| **0** | Enquadrar / zoom máximo |
| **F** | Seguir o player |
| **G** | Grade |
| **M** | Marcar posição |
| **C** | Limpar marcas |
| **Tab** | Fechar o mapa |
| **IDDT** | Cheat: todas as paredes, depois as things (digite com o mapa aberto) |

### Menu e teclas de função

| Tecla | Ação |
|---|---|
| **Esc** | Menu |
| **Enter** | Confirmar / avançar |
| **Backspace** | Voltar |
| **Y** / **N** | Sim / Não |
| **F2** | Save |
| **F3** | Load |
| **F11** | Liga/desliga overlay de FPS |
| **+** / **-** | Screen Size (igual às Options) com o automap fechado |
| **Alt+Enter** | Tela cheia |

**Screen Size** e **Graphic Detail** (HIGH/LOW) mudam a vista 3D (`R_SetViewSize`), não a escala da janela. Escala da janela: arraste a janela ou Alt+Enter.

### Cheats (só nostalgia)

Digite no teclado durante o jogo; não precisa de Enter:

| Código | Efeito |
|---|---|
| **IDDQD** | God mode (_Degreelessness Mode_); cara do HUD `STFGOD0` |
| **IDKFA** | Todas as armas, munição, chaves e armadura |
| **IDFA** | Armas, munição e armadura (sem chaves) |
| **IDCLIP** / **IDSPISPOPD** | Sem clip |
| **IDDT** | Cheat do automap (com o mapa aberto): todas as paredes, depois as things |

---

## Parâmetros de linha de comando

### IWAD

| Parâmetro | Descrição |
|---|---|
| `-iwad file.wad` | IWAD a carregar (caminho ou nome) |
| `file.wad` | A mesma coisa, sem `-iwad` |

### Vídeo

| Parâmetro | Descrição |
|---|---|
| `-fullscreen` | Começa em janela cheia |
| `-crt` | Visual CRT com scanlines (família `-crt` do Harbour). Fica mais claro em escala 2× ou mais |
| `-fps` | Mostra frames por segundo no HUD (canto superior direito) e no título da janela. **F11** liga/desliga |

### Jogo

| Parâmetro | Descrição |
|---|---|
| `-warp e m` | Pula o título e começa o episódio `e` mapa `m` |
| `-skill n` | 0 baby … 4 nightmare (padrão 2, Hurt Me Plenty) |
| `-nomonsters` | Não spawna inimigos |

Flags só do Harbour **não** implementadas aqui: `-videoc`, `-scaling`, `-gfxmode`, `-colors`, `-nosound` / `-nosfx` / `-nomusic`, `-config`, rede/CD/joystick.

---

## Layout

```
src/doom/Doom.java   entrada: java doom.Doom
run.bat              atalho Windows (javac + IWAD padrão)
src/doom/            motor (Java 17)
lib/                 jar do JNA + coloque SDL2.dll aqui
screenshot/doom.png  screenshot do README
docs/                QR codes de doação
```

| Caminho | Vanilla / Harbour |
|---|---|
| `Compat.java` | `m_fixed`, `xhb_compat` |
| `Defs.java` | `doomdef`, `doomtype`, `doomkeys` |
| `Bin.java` | leitores little-endian de WAD / mapa |
| `Keys.java` | `doomkeys` / keycodes SDL |
| `Wad.java` | `w_wad` |
| `Video.java` | `i_video`, `doomgeneric_allegro` (SDL2 + `-crt`) |
| `Sdl2.java` | bindings JNA do SDL2 |
| `VVideo.java` | `v_video` |
| `Tables.java` | `tables` |
| `Resources.java` | `r_data` |
| `Renderer.java` | `r_main` `r_bsp` `r_segs` `r_plane` `r_draw` |
| `World.java` | `p_setup` |
| `Collision.java` | `p_map` `p_maputl` `p_sight` (REJECT) |
| `Player.java` | `p_user` `p_pspr` |
| `Specials.java` | `p_spec` `p_doors` `p_plats` `p_floor` `p_switch` `p_telept` |
| `Mobj.java` | `info` `p_inter` |
| `Sprites.java` | `r_things` |
| `Enemy.java` | `p_enemy` (look / chase / ataque) |
| `Status.java` | `st_stuff` |
| `AmMap.java` | `am_map` |
| `Sound.java` | `i_sound`, CacheSFX, MIDI Java Sound |
| `Mus2Mid.java` | `mus2mid.c` |
| `Menu.java` | `m_menu` |
| `Saveg.java` | `p_saveg` (nome de 24 bytes + JSON) |
| `Intermission.java` | `wi_stuff` |
| `Wipe.java` | `f_wipe` melt |
| `Finale.java` | `f_finale` (texto + bunny scroll) |
| `Game.java` | `d_main` `g_game` `d_loop` `boot` |

---

## Linhagem

1. **id Software DOOM** (1993) — motor original
2. **Chocolate Doom / doomgeneric** — C portátil
3. **[harbour_doom](https://github.com/vagucs/harbour_doom)** — Harbour + Allegro 4.2.2 (`Doom_hb.exe`)
4. **doom_python** — Python + pygame, a partir desse port Harbour
5. **php_doom** — PHP 8 CLI + SDL2 FFI
6. **node_doom** — Node.js CLI + TypeScript + SDL2 (koffi)
7. **Esta árvore** — Java 17 CLI + SDL2 (JNA), a partir do mesmo Harbour (intenção de gameplay 100% dos `.prg`; quantidade de arquivos condensada)

---

## Doe

### Ethereum

`0x1b64038A2b1DB73ABd0068d8B9B0d1dC5a90C5F1`

![QR Code Ethereum](docs/qr-ethereum.png)

### PIX

Chave: `vagucs@bol.com.br`

![QR Code PIX](docs/qr-pix.png)
