# Overworld-Agent

Overworld-Agent is a Minecraft plugin to create and define agent behavior. 

_**Requires Java 11+**_

---

## Building
Build the source with Maven:
```
$ mvn install
```

---

## Configuration
The config file can be found under `/Overworld-Agent/src/main/resources/config.yml`.

### Skins
| Key | Type | Description |
|---|---|---|
|`skins.<skin name>`|`string`|Unique name for the skin can be made up by the researcher|
|`skins.<skin name>.signature`|`string`|The texture signature of the custom skin from https://mineskin.org|
|`skins.<skin name>.data`|`string`|The texture value of the custom skin from https://mineskin.org|


#### Example
```yaml
skins:
  <skin name>:
    signature: <texture signature of custom skin>
    data: <texture value of custom skin>
```
---

## Commands
| Command                                                                                | Description                                                                                                                          |
|----------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------|
| `/agent spawn <skin name> <agent name>`                                  | Spawn an agent with the specified skin and name that follows the student that spawned the agent and when right clicked prompts for a discussion.  |
| `/agent reactivate <player name or all>`                                  | Respawns an agent for the specified player or all players on the server when argument is all.|
| `/agent despawn <player name or all>`                                  | Despawns an agent for the specified player or all players on the server when argument is all.|
| `/agent destroy <player name or all>`                                  | Destroys an agent for the specified player or all players on the server when argument is all.|
| `/agent name <agent name>`                                  | Changes the agent's name for the student.|
| `/agent skin <skin>`                                  | Changes the agent's skin for the student.|

### Skin Types
Input for `<skin name>` make sure spelled correctly and all lowercase
| Name | Description |
|---|---|
| `astronaut` | Ambiguous gender and ethnicity agent in astronaut suit. |
| `wmscientist` | White male scientist. |
| `wfscientist` | White female scientist. |
| `bmscientist` | Black male scientist. |
| `bfscientist` | Black female scientist. |
| `amscientist` | Asian male scientist. |
| `afscientist` | Asian female scientist. |
| `hmscientist` | Hispanic male scientist. |
| `hfscientist` | Hispanic female scientist. |

