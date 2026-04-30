cmake_minimum_required(VERSION 3.22)
project(speex_jni C)

set(SPEEX_DIR ${CMAKE_CURRENT_SOURCE_DIR}/speex)

add_library(speex_jni SHARED
    speex_jni.c
    ${SPEEX_DIR}/bits.c
    ${SPEEX_DIR}/cb_search.c
    ${SPEEX_DIR}/exc_10_16_table.c
    ${SPEEX_DIR}/exc_10_32_table.c
    ${SPEEX_DIR}/exc_20_32_table.c
    ${SPEEX_DIR}/exc_5_256_table.c
    ${SPEEX_DIR}/exc_5_64_table.c
    ${SPEEX_DIR}/exc_8_128_table.c
    ${SPEEX_DIR}/filters.c
    ${SPEEX_DIR}/gain_table.c
    ${SPEEX_DIR}/gain_table_lbr.c
    ${SPEEX_DIR}/hexc_10_32_table.c
    ${SPEEX_DIR}/hexc_table.c
    ${SPEEX_DIR}/high_lsp_tables.c
    ${SPEEX_DIR}/kiss_fft.c
    ${SPEEX_DIR}/kiss_fftr.c
    ${SPEEX_DIR}/lpc.c
    ${SPEEX_DIR}/lsp.c
    ${SPEEX_DIR}/lsp_tables_nb.c
    ${SPEEX_DIR}/ltp.c
    ${SPEEX_DIR}/modes.c
    ${SPEEX_DIR}/modes_wb.c
    ${SPEEX_DIR}/nb_celp.c
    ${SPEEX_DIR}/quant_lsp.c
    ${SPEEX_DIR}/sb_celp.c
    ${SPEEX_DIR}/smallft.c
    ${SPEEX_DIR}/speex.c
    ${SPEEX_DIR}/speex_callbacks.c
    ${SPEEX_DIR}/speex_header.c
    ${SPEEX_DIR}/stereo.c
    ${SPEEX_DIR}/vbr.c
    ${SPEEX_DIR}/vq.c
    ${SPEEX_DIR}/window.c
)

target_include_directories(speex_jni PRIVATE
    ${SPEEX_DIR}
    ${CMAKE_CURRENT_SOURCE_DIR}/include
)

target_compile_definitions(speex_jni PRIVATE
    HAVE_CONFIG_H
    FLOATING_POINT
    EXPORT=__attribute__\(\(visibility\("default"\)\)\)
)

find_library(log-lib log)
target_link_libraries(speex_jni ${log-lib})
