//
// Created by lshq on 2017/7/10.
//

#define LOG_TAG "SpiStub"
#include <fcntl.h>
#include<android/log.h>
#include <errno.h>
#include "spihal.h"
#include <sys/stat.h>
#include <sys/time.h>
#include <sys/ioctl.h>
#include <unistd.h>

#include <stdlib.h>

/* 定义变量 */
static uint8_t mode = 0;
static uint8_t bits = 8;
static uint32_t speed = 600*1000;
static uint16_t delay = 0;
struct spi_ioc_transfer spi_tr;

/* 定义初始化spi */
int hal_spi_init(int fd)
{
    /* mode */
    if(ioctl(fd, SPI_IOC_WR_MODE, &mode) < 0)
    {
        ALOGE("SpiStub: SPI wr_mode error\n");
    }
    else
        ALOGE("SpiStub: SPI wr_mode ok\n");

    if(ioctl(fd, SPI_IOC_RD_MODE, &mode) < 0)
    {
        ALOGE("SpiStub: SPI rd_mode error\n");
    }
    else
        ALOGE("SpiStub: SPI rd_mode ok\n");

    /* bits */
    if(ioctl(fd, SPI_IOC_WR_BITS_PER_WORD, &bits) < 0)
    {
        ALOGE("SpiStub: SPI wr_bits_per_word error\n");
    }
    else
        ALOGE("SpiStub: SPI wr_bit_per_word ok\n");

    if(ioctl(fd, SPI_IOC_RD_BITS_PER_WORD, &bits) < 0)
    {
        ALOGE("SpiStub: SPI rd_bit_per_word error\n");
    }
    else
        ALOGE("SpiStub: SPI rd_bit_per_word ok\n");

    /* speed */
    if(ioctl(fd, SPI_IOC_WR_MAX_SPEED_HZ, &speed) < 0)
    {
        ALOGE("SpiStub: SPI wr_max_speed_hz error\n");
    }
    else
        ALOGE("SpiStub: SPI wr_max_speed_hz ok\n");

    if(ioctl(fd, SPI_IOC_RD_MAX_SPEED_HZ, &speed) < 0)
    {
        ALOGE("SpiStub: SPI rd_max_speed_hz error\n");
    }
    else
        ALOGE("SpiStub: SPI rd_max_speed_hz ok\n");

    return 0;
}
/* 定义spi读函数 */
int hal_spi_read(int fd, unsigned char *data, int size)
{
    spi_tr.rx_buf = (unsigned long)data;
    spi_tr.len = (unsigned int)size;
    spi_tr.delay_usecs = delay;

    if((ioctl(fd, SPI_IOC_MESSAGE(1), &spi_tr)) < 0)
    {
        ALOGE("SpiStub: SPI read  error!\n");
        return -1;
    }
    return 0;
}
/* 定义spi写函数 */
int hal_spi_write(int fd, unsigned char *data, int size)
{
    spi_tr.tx_buf = (unsigned long)data;
    spi_tr.rx_buf = NULL;
    spi_tr.len = (unsigned int)size;
    spi_tr.delay_usecs = delay;

    if((ioctl(fd, SPI_IOC_MESSAGE(1), &spi_tr)) < 1)
    {
        ALOGE("SpiStub: SPI write error!\n");
        return -1;
    }
    return 0;
}
/* 定义spi设备打开函数 */
int hal_spi_open(int *fd, char const *name)
{
    if((*fd = open(DEVICE_NAME, O_RDWR)) < 0)
    {
        ALOGE("SpiStub: failed spi open .\n");
        return -1;
    }
    ALOGE("SpiStub: open /dev/spi1.0 successfuly .\n");
    return 0;
}
/* 定义spi设备关闭函数 */
int hal_spi_close(int fd)
{

    if(fd>0)
    {
        ALOGE("SpiStub: close /dev/spi1.0\n");
        close(fd);
    }
    return 0;
}
