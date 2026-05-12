package io.codepieces.tyche.recommendations.tracking;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recommendations/tracked-stocks")
public class TrackedStockController {

	private final TrackedStockService trackedStockService;

	public TrackedStockController(TrackedStockService trackedStockService) {
		this.trackedStockService = trackedStockService;
	}

	@GetMapping
	public List<TrackedStock> list() {
		return trackedStockService.allTrackedStocks();
	}

	@GetMapping("/{symbol}")
	public TrackedStock get(@PathVariable String symbol) {
		return trackedStockService.trackedStock(symbol);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public TrackedStock create(@RequestBody TrackedStockRequest request) {
		return trackedStockService.create(request);
	}

	@PutMapping("/{symbol}")
	public TrackedStock update(@PathVariable String symbol, @RequestBody TrackedStockRequest request) {
		return trackedStockService.update(symbol, request);
	}

	@PatchMapping("/{symbol}/enabled")
	public TrackedStock setEnabled(@PathVariable String symbol, @RequestBody Map<String, Boolean> request) {
		Boolean enabled = request.get("enabled");
		if (enabled == null) {
			throw new InvalidTrackedStockException("enabled must be provided");
		}
		return trackedStockService.updateEnabled(symbol, enabled);
	}

	@DeleteMapping("/{symbol}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable String symbol) {
		trackedStockService.delete(symbol);
	}

	@ExceptionHandler(TrackedStockNotFoundException.class)
	@ResponseStatus(HttpStatus.NOT_FOUND)
	public Map<String, String> notFound(TrackedStockNotFoundException ex) {
		return Map.of("error", ex.getMessage());
	}

	@ExceptionHandler(InvalidTrackedStockException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public Map<String, String> invalid(InvalidTrackedStockException ex) {
		return Map.of("error", ex.getMessage());
	}
}
