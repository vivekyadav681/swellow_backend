package com.elsewhere.swellow.blockchain;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/blockchain")
public class BlockchainController {

    private final BlockchainService blockchainService;
    private final MiningService miningService;

    public BlockchainController(BlockchainService blockchainService, MiningService miningService) {
        this.blockchainService = blockchainService;
        this.miningService = miningService;
    }

    @PostMapping("/mine")
    public ResponseEntity<Block> mine() {
        return ResponseEntity.ok(miningService.minePendingTransactions());
    }

    @GetMapping("/blocks")
    public ResponseEntity<List<Block>> getBlocks() {
        return ResponseEntity.ok(blockchainService.getBlocks());
    }

    @GetMapping("/blocks/{id}")
    public ResponseEntity<Block> getBlock(@PathVariable Long id) {
        return blockchainService.getBlockById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/validate")
    public ResponseEntity<Map<String, Object>> validate() {
        boolean valid = blockchainService.validateChain();
        return ResponseEntity.ok(Map.of(
                "valid", valid,
                "message", valid ? "Blockchain is valid and intact." : "Blockchain validation failed!"
        ));
    }
}
