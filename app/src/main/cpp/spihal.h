//
// Created by lshq on 2017/7/10.
//

#ifndef SPITEST_SPIHAL_H
#define SPITEST_SPIHAL_H

#include <linux/spi/spidev.h>

#include <fcntl.h>

#define DEVICE_NAME "/dev/spidev1.0"
#define MODULE_NAME "SPIDEV"
#define ALOGE(...)  __android_log_print(ANDROID_LOG_ERROR,MODULE_NAME,__VA_ARGS__)

/* 函数声明 */

int hal_spi_init(int fd);
int hal_spi_read(int fd, unsigned char *data, int size);
int hal_spi_write(int fd, unsigned char *data, int size);
int hal_spi_open(int *fd, char const *name);
int hal_spi_close(int fd);


#endif //SPITEST_SPIHAL_H
