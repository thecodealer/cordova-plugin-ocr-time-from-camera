import UIKit
import Vision

@objc(OcrTimeFromCamera) class OcrTimeFromCamera: CDVPlugin, OcrScannerDelegate {
    private var jsCallbackId: String?
    private var isRunning: Bool = false
    private var scannerViewController: OcrScannerViewController?
    private var scannerStoryboard: UIStoryboard?

    @objc(start:)
    func start(command: CDVInvokedUrlCommand) {
        jsCallbackId = command.callbackId
        commandDelegate.run(inBackground: {
            DispatchQueue.main.async(execute: {
                self.openCameraView()
            })
        })
    }
    
    private func openCameraView() {
        if (isRunning == true) {
            returnError(errorId: "running", errorMessage: "Scanner is already running", paramsJson: "")
        }
        else {
            isRunning = true
            returnSuccess(event: "started", paramsJson: "")

            scannerStoryboard = UIStoryboard(name: "OcrTimeFromCameraMain", bundle: nil)
            scannerViewController = scannerStoryboard?.instantiateViewController(identifier: "OcrScannerView") { (coder) in
                return OcrScannerViewController(coder: coder)
            }
            scannerViewController?.delegate = self
            scannerViewController?.modalPresentationStyle = .fullScreen
            viewController.present(scannerViewController!, animated: true, completion: nil)
        }
    }
    
    private func returnResponse(type: String?, response: [AnyHashable : Any]?) {
        var result: CDVPluginResult?
        if (type == "success") {
            result = CDVPluginResult(status: CDVCommandStatus_OK, messageAs: response)
        }
        else {
            result = CDVPluginResult(status: CDVCommandStatus_ERROR, messageAs: response)
        }
        result?.setKeepCallbackAs(true)
        commandDelegate!.send(result, callbackId: jsCallbackId)
    }

    private func returnSuccess(event: String?, paramsJson: String?) {
        let params = [
            "event" : event ?? "",
            "data" : paramsJson ?? ""
        ]
        returnResponse(type: "success", response: params)
    }
    
    private func returnError(errorId: String?, errorMessage: String?, paramsJson: String?) {
        let params = [
            "id" : errorId ?? "",
            "error" : errorMessage ?? "",
            "params" : paramsJson ?? ""
        ]
        returnResponse(type: "error", response: params)
    }
    
    func onScannerResult(data: String) {
        DispatchQueue.main.async {
            self.scannerViewController?.dismiss(animated: true, completion: nil)
        }
        returnSuccess(event: "captured", paramsJson: data)
        isRunning = false
        returnSuccess(event: "stopped", paramsJson: "")
    }
}

protocol OcrScannerDelegate: class {
    func onScannerResult(data: String)
}

class OcrScannerViewController: VisionViewController {
    weak var delegate: OcrScannerDelegate?
    var textRequest: VNRecognizeTextRequest!
    var timeFound: Bool = false
    var matchedTimePatterns = [String: Int]();

    override func viewDidLoad() {
        textRequest = VNRecognizeTextRequest(completionHandler: recognizeTextHandler)
        super.viewDidLoad()
    }
    
    @IBAction override func handleTap(_ sender: UITapGestureRecognizer) {
    }

    override func recognizeTextHandler(request: VNRequest, error: Error?) {
        if !timeFound {
            var numbers = [String]()
            var redBoxes = [CGRect]()
            var greenBoxes = [CGRect]()
            
            guard let results = request.results as? [VNRecognizedTextObservation] else {
                return
            }
            
            let maximumCandidates = 1
            
            for visionResult in results {
                guard let candidate = visionResult.topCandidates(maximumCandidates).first else { continue }
                
                var numberIsSubstring = true
                
                if let result = candidate.string.extractTime() {
                    let (range, time) = result
                    if let box = try? candidate.boundingBox(for: range)?.boundingBox {
                        numbers.append(time)
                        greenBoxes.append(box)
                        numberIsSubstring = !(range.lowerBound == candidate.string.startIndex && range.upperBound == candidate.string.endIndex)
                        let timePattern = String(time.dropLast())
                        if isBestMatch(timePattern) {
                            onTimeFound(time)
                        }
                        
                        if matchedTimePatterns[timePattern] != nil {
                            matchedTimePatterns[timePattern] = matchedTimePatterns[timePattern]! + 1
                        }
                        else {
                            matchedTimePatterns[timePattern] = 1
                        }
                    }
                }
                if numberIsSubstring {
                    redBoxes.append(visionResult.boundingBox)
                }
            }
            
            numberTracker.logFrame(strings: numbers)
            show(boxGroups: [(color: UIColor.red.cgColor, boxes: redBoxes), (color: UIColor.green.cgColor, boxes: greenBoxes)])
            
            // Check if we have any temporally stable numbers.
            if let sureNumber = numberTracker.getStableString() {
                showString(string: sureNumber)
                numberTracker.reset(string: sureNumber)
            }
        }
    }
    
    func onTimeFound(_ time: String) {
        timeFound = true
        matchedTimePatterns.removeAll()
        delegate?.onScannerResult(data: time)
    }
    
    func isBestMatch(_ pattern: String) -> Bool {
        var output: Bool = false;
        if matchedTimePatterns[pattern] != nil {
            output = matchedTimePatterns[pattern]! > 2
        }
        return output;
    }
}

extension String {
    func extractTime() -> (Range<String.Index>, String)? {
        // let pattern = #"(^|\s)\d{1,3}(:|\s|\.)\d{2}($|\s)"#
        let pattern = #"(^|\s)\d{2,3}(:|\.)\d{2}($|\s)"#
        let string = self
        
        guard let range = string.range(of: pattern, options: .regularExpression, range: nil, locale: nil) else {
            return nil
        }
        
        let result: String = String(string[range]).trimmingCharacters(in: .whitespacesAndNewlines).replacingOccurrences(of: ".", with: ":").replacingOccurrences(of: " ", with: ":")


//        do {
//            let regex = try NSRegularExpression(pattern: pattern)
//            let match = regex.firstMatch(in: string, range: NSRange(0..<string.utf16.count))
//            let matchedRange = Range(match!.range, in: string)!
//            let matchedTime = String(string[range]).trimmingCharacters(in: .whitespacesAndNewlines).replacingOccurrences(of: ".", with: ":").replacingOccurrences(of: " ", with: ":")
//
//            result = matchedTime
//        }
//        catch {
//            print("Regex was bad!")
//        }
        
        guard !result.isEmpty else {
            return nil
        }

        return (range, result)
    }
}
