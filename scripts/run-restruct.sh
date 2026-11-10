#!/bin/bash
# Последовательный прогон задач реструктуризации через Claude Code CLI.
# Использование: run-restruct.sh R-A [R-B] [R-C]   (по умолчанию R-A)
# Повторяет запуск, если Claude упёрся в 5-часовой лимит: до 4 попыток с паузой 10 минут.
cd /root/palma || exit 1
mkdir -p logs
CL=/root/.hermes/tools/node-26.12.1-linux-x64/bin/claude
LOG=logs/restruct.log

TASKS=("$@")
[ ${#TASKS[@]} -eq 0 ] && TASKS=(R-A)

echo "старт $(date -u '+%F %H:%M UTC')" >> "$LOG"

for t in "${TASKS[@]}"; do
  D=docs/TASK-$t.md
  if [ ! -f "$D" ]; then
    echo "=== $t: нет ТЗ $D ===" >> "$LOG"
    continue
  fi
  attempt=1
  while [ $attempt -le 12 ]; do
    echo "=== $t: начало $(date -u '+%H:%M:%S') (попытка $attempt) ===" >> "$LOG"
    IS_SANDBOX=1 "$CL" --model claude-opus-5-5 --dangerously-skip-permissions \
      --print "$(cat "$D")" > "logs/task-$t.log" 2>&1
    code=$?
    if grep -q "5-hour limit" "logs/task-$t.log"; then
      echo "=== $t: лимит Claude, пауза 10 мин ===" >> "$LOG"
      sleep 600
      attempt=$((attempt+1))
      continue
    fi
    echo "=== $t: конец, код $code ===" >> "$LOG"
    break
  done
done

echo "готово $(date -u '+%F %H:%M UTC')" >> "$LOG"
