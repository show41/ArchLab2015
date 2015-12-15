`timescale 1ns / 1ps

/**
 * @brief ifid 流水段寄存器
 * @author whz
 *
 * pc + 4直接实现在内部了
 */

`define PC_WIDTH 32
`define INSTR_WIDTH 32
`define PC_BUS 31:0
`define DATA_BUS 31:0
`define JMP_BUS 31:0
`define REG_ADDR_BUS 4:0
`define IMM_BUS 15:0
`define JMP_SLICE 25:0
`define JMP_HEAD_SLICE 31:28
`define RS_SLICE 25:21
`define RT_SLICE 20:16
`define RD_SLICE 15:11
`define IMM_SLICE 15:0

module ifid_reg(
    input clk,
    input reset,
    input cu_stall,
    input cu_flush,
    input [`PC_BUS] pc,
    input [`DATA_BUS] instr,
    output reg [`PC_BUS] ifid_pc,
    output reg [`PC_BUS] ifid_pc_4,
    output reg [`DATA_BUS] ifid_instr,
    output [`JMP_BUS] ifid_jump_addr,
    output [`REG_ADDR_BUS] ifid_rs_addr,
    output [`REG_ADDR_BUS] ifid_rt_addr,
    output [`REG_ADDR_BUS] ifid_rd_addr,
    output [`IMM_BUS] ifid_imm
    );

    initial begin
        ifid_pc    = `PC_WIDTH'd0;
        ifid_pc_4  = `PC_WIDTH'd4;
        ifid_instr = `PC_WIDTH'd0;
    end

    assign ifid_jump_addr = { ifid_pc[`JMP_HEAD_SLICE], ifid_instr[`JMP_SLICE], 2'b0 };
    assign ifid_rs_addr   = ifid_instr[`RS_SLICE];
    assign ifid_rt_addr   = ifid_instr[`RT_SLICE];
    assign ifid_rd_addr   = ifid_instr[`RD_SLICE];
    assign ifid_imm       = ifid_instr[`IMM_SLICE];
    
    always @(negedge clk or posedge reset) begin
        if (reset || cu_flush) begin
            ifid_pc    <= `PC_WIDTH'd0;
            ifid_pc_4  <= `PC_WIDTH'd4;
            ifid_instr <= `PC_WIDTH'd0;
        end
        else if (cu_stall) begin
            ifid_pc    <= ifid_pc;
            ifid_pc_4  <= ifid_pc_4;
            ifid_instr <= ifid_instr;
        end
        else begin
            ifid_pc    <= pc;
            ifid_pc_4  <= pc + 4;
            ifid_instr <= instr;
        end
    end

endmodule

`undef PC_WIDTH
`undef INSTR_WIDTH
`undef PC_BUS
`undef DATA_BUS
`undef JMP_SLICE
`undef RS_SLICE
`undef RT_SLICE
`undef RD_SLICE
