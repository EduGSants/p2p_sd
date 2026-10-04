#!/bin/bash
export LC_ALL=C
set -e

if [ $# -lt 6 ]; then
    echo "Uso: $0 <classeServidor> <porta> <arquivo> <nClientes> <nRepeticoes> <saida.csv>"
    exit 1
fi

SERVER_CLASS=$1
PORT=$2
FILE=$3
N_CLIENTS=$4
N_REPS=$5
OUTPUT=$6

mkdir -p "$(dirname "$OUTPUT")"

if [ ! -f "$OUTPUT" ]; then
    echo "arquitetura,arquivo,n_clientes,repeticao,min,med,max" > "$OUTPUT"
fi

ARQ_BASENAME=$(basename "$FILE")
ARQ_NOME=${SERVER_CLASS#cs.}

for rep in $(seq 1 "$N_REPS"); do

    # --- Garante que a porta está livre antes de subir o servidor ---
    if ss -ltn 2>/dev/null | grep -q ":$PORT "; then
        echo "Porta $PORT ainda ocupada, tentando matar processo antigo..."
        fuser -k -n tcp "$PORT" 2>/dev/null || true
        sleep 1
    fi

    # --- Sobe o servidor (guardando log em vez de /dev/null) ---
    LOG_SERVER="resultados/_log_servidor_${ARQ_NOME}_${PORT}.txt"
    java -cp bin "$SERVER_CLASS" "$FILE" > "$LOG_SERVER" 2>&1 &
    SERVER_PID=$!

    # --- Espera a porta abrir (máx 5s) ---
    OPEN=0
    for i in $(seq 1 50); do
        if ss -ltn 2>/dev/null | grep -q ":$PORT "; then
            OPEN=1
            break
        fi
        # se o servidor morreu, aborta
        if ! kill -0 "$SERVER_PID" 2>/dev/null; then
            echo "ERRO: servidor morreu ao iniciar. Veja $LOG_SERVER"
            cat "$LOG_SERVER"
            exit 1
        fi
        sleep 0.1
    done

    if [ "$OPEN" -ne 1 ]; then
        echo "ERRO: porta $PORT não abriu em 5s."
        kill -9 "$SERVER_PID" 2>/dev/null || true
        exit 1
    fi

    # --- Dispara N clientes ---
    TMPDIR=$(mktemp -d)
    PIDS=()

    for c in $(seq 1 "$N_CLIENTS"); do
        java -cp bin cs.Cliente 127.0.0.1 "$PORT" > "$TMPDIR/cli_$c.txt" 2>&1 &
        PIDS+=($!)
    done

    for pid in "${PIDS[@]}"; do
        wait "$pid" 2>/dev/null || true
    done

    # --- Mata o servidor (TERM, depois KILL) ---
    kill "$SERVER_PID" 2>/dev/null || true
    for i in $(seq 1 20); do
        kill -0 "$SERVER_PID" 2>/dev/null || break
        sleep 0.1
    done
    kill -9 "$SERVER_PID" 2>/dev/null || true

    # --- Extrai tempos ---
    TEMPOS=$(grep -h "Tempo:" "$TMPDIR"/cli_*.txt | awk '{print $5}')
    N_TEMPOS=$(echo "$TEMPOS" | wc -l)

    if [ "$N_TEMPOS" -ne "$N_CLIENTS" ]; then
        echo "AVISO: esperava $N_CLIENTS tempos, obteve $N_TEMPOS"
        echo "--- conteúdo dos clientes ---"
        cat "$TMPDIR"/cli_*.txt
        echo "-----------------------------"
    fi

    rm -rf "$TMPDIR"

    STATS=$(echo "$TEMPOS" | awk '
        NR==1 { min=$1; max=$1; soma=0 }
        { if ($1<min) min=$1; if ($1>max) max=$1; soma+=$1; n++ }
        END  { if (n>0) printf "%.4f,%.4f,%.4f", min, soma/n, max; else print "0,0,0" }
    ')

    echo "${ARQ_NOME},${ARQ_BASENAME},${N_CLIENTS},${rep},${STATS}" >> "$OUTPUT"
    echo "[$ARQ_NOME | $ARQ_BASENAME | ${N_CLIENTS} clientes | rep $rep/$N_REPS] $STATS"
done