# Skill: Convención de Commits y Mensajes de Git (Conventional Commits)

## Propósito
Esta skill establece la estructura y reglas obligatorias que el agente de IA debe seguir al generar propuestas de mensajes de commit en Git para el proyecto LifterLab. El objetivo es mantener un historial limpio, trazable con las historias de usuario (RF/US) y listo para integraciones continuas.

---

## 1. Estructura del Mensaje de Commit

Cada mensaje de commit debe seguir el estándar **Conventional Commits**:
as
```text
<tipo>(<módulo/alcance>): <descripción corta en presente e imperativo> [#ID_ISSUE]

[cuerpo opcionsal explicativo si el cambio es complejo]