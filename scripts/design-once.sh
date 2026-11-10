#!/bin/bash
# Отдельный прогон дизайна окна входа дизайн-слоем (omd-orchestrator), параллельно цепочке.
cd /root/palma || exit 1
CL=/root/.hermes/tools/node-26.12.1-linux-x64/bin/claude
L=logs/design-extra.log
echo "=== DESIGN (omd-orchestrator): начало $(date -u '+%H:%M:%S') ===" > $L
IS_SANDBOX=1 "$CL" --agent omd-orchestrator --model claude-opus-5-5 --dangerously-skip-permissions \
  --print "$(cat docs/TASK-LOGIN-DESIGN.md)" > logs/task-DESIGN.log 2>&1
echo "=== DESIGN: конец, код $? ===" >> $L
echo "готово $(date -u '+%H:%M UTC')" >> $L
