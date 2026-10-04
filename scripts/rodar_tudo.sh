#!/bin/bash
export LC_ALL=C
set -e

REPS=3
ARQUIVOS=(arquivos/arquivo_5mb.bin arquivos/arquivo_50mb.bin arquivos/arquivo_500mb.bin)
CLIENTES=(1 2 4 8)

rm -rf resultados
mkdir -p resultados

# ---------- Sequencial ----------
for arq in "${ARQUIVOS[@]}"; do
    for n in "${CLIENTES[@]}"; do
        bash scripts/rodar_experimento.sh cs.ServidorSequencial 5001 \
            "$arq" "$n" "$REPS" "resultados/sequencial.csv"
    done
done

# ---------- Multithread ----------
for arq in "${ARQUIVOS[@]}"; do
    for n in "${CLIENTES[@]}"; do
        bash scripts/rodar_experimento.sh cs.ServidorMultiThread 5002 \
            "$arq" "$n" "$REPS" "resultados/multithread.csv"
    done
done

# ---------- ThreadPool ----------
for arq in "${ARQUIVOS[@]}"; do
    for n in "${CLIENTES[@]}"; do
        bash scripts/rodar_experimento.sh cs.ServidorThreadPool 5003 \
            "$arq" "$n" "$REPS" "resultados/threadpool.csv"
    done
done

echo "Tudo pronto. Veja resultados/."